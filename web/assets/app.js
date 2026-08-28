(function () {
    'use strict';

    document.querySelectorAll('[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm(form.getAttribute('data-confirm'))) event.preventDefault();
        });
    });

    var toggle = document.querySelector('[data-nav-toggle]');
    var sidebar = document.querySelector('[data-sidebar]');
    var mask = document.querySelector('[data-sidebar-mask]');
    function closeSidebar() {
        if (sidebar) sidebar.classList.remove('open');
        if (mask) mask.classList.remove('open');
    }
    if (toggle && sidebar) {
        toggle.addEventListener('click', function () {
            sidebar.classList.toggle('open');
            if (mask) mask.classList.toggle('open');
        });
    }
    if (mask) mask.addEventListener('click', closeSidebar);

    var currentPath = window.location.pathname;
    document.querySelectorAll('.side-nav a').forEach(function (link) {
        var path = new URL(link.href, window.location.origin).pathname;
        if (currentPath === path || (path.indexOf('/dashboard') < 0 && currentPath.indexOf(path) === 0)) {
            link.classList.add('active');
        }
    });

    var device = document.querySelector('select[name="deviceId"]');
    var fault = document.querySelector('select[name="faultId"]');
    if (device && fault) {
        device.addEventListener('change', function () {
            Array.prototype.forEach.call(fault.options, function (option) {
                option.hidden = !!option.dataset.device && option.dataset.device !== device.value;
            });
            fault.value = '';
        });
    }

    var repairForm = document.querySelector('[data-ai-repair-form]');
    if (repairForm) {
        var search = repairForm.querySelector('[data-problem-search]');
        var diagnoseButton = repairForm.querySelector('[data-ai-diagnose]');
        var aiResult = repairForm.querySelector('[data-ai-result]');
        var selectedIssue = repairForm.querySelector('[data-selected-issue]');
        var issueCards = Array.prototype.slice.call(repairForm.querySelectorAll('[data-issue-card]'));
        var deviceSelect = repairForm.querySelector('select[name="deviceId"]');
        var faultSelect = repairForm.querySelector('select[name="faultId"]');
        var description = repairForm.querySelector('textarea[name="description"]');
        var aiEndpoint = repairForm.dataset.aiEndpoint;
        var aiRequestSeq = 0;

        var keywordMap = [
            { keys: ['开不了', '打不开', '不开机', '黑屏', '启动', '电源', '无法开机'], hint: '建议先检查电源、插座、适配器和指示灯状态。若仍无反应，通常需要工程师现场检测电源模块或主板。' },
            { keys: ['系统', '蓝屏', '卡顿', '死机', '报错', '崩溃', '中毒', '很慢'], hint: '这类问题可能来自系统文件、驱动或存储异常。建议提前记录报错提示，工程师可现场做系统诊断。' },
            { keys: ['打印', '打印机', '卡纸', '墨盒', '硒鼓', '不出纸', '脱机'], hint: '建议先确认纸张、墨盒/硒鼓、网络连接和打印队列。平台会优先推荐打印设备工程师。' },
            { keys: ['冰箱', '不制冷', '制冷', '冷冻', '温度', '结冰', '家电', '空调'], hint: '制冷异常可能与压缩机、制冷剂、温控或风道有关，建议预约具备家电维修经验的工程师。' }
        ];

        function normalize(text) {
            return (text || '').toLowerCase().replace(/\s+/g, '');
        }

        function cardText(card) {
            return normalize([
                card.dataset.deviceName,
                card.dataset.faultName,
                card.textContent
            ].join(' '));
        }

        function scoreCard(card, query) {
            var text = cardText(card);
            var score = 0;
            if (!query) return 0;
            if (text.indexOf(query) >= 0) score += 8;
            keywordMap.forEach(function (group) {
                group.keys.forEach(function (key) {
                    if (query.indexOf(key) >= 0 && text.indexOf(key) >= 0) score += 6;
                    else if (query.indexOf(key) >= 0) {
                        if (text.indexOf('开机') >= 0 && ['开不了', '打不开', '不开机', '黑屏', '启动', '电源'].indexOf(key) >= 0) score += 4;
                        if (text.indexOf('系统') >= 0 && ['蓝屏', '卡顿', '死机', '报错', '崩溃', '中毒', '很慢'].indexOf(key) >= 0) score += 4;
                        if (text.indexOf('打印') >= 0 && ['打印机', '卡纸', '墨盒', '硒鼓', '不出纸', '脱机'].indexOf(key) >= 0) score += 4;
                        if (text.indexOf('制冷') >= 0 && ['冰箱', '不制冷', '冷冻', '温度', '结冰', '家电', '空调'].indexOf(key) >= 0) score += 4;
                    }
                });
            });
            return score;
        }

        function findHint(query) {
            var fallback = '我已按你的描述匹配平台服务类型。下一步会展示技能、区域和可预约时间都符合的工程师。';
            for (var i = 0; i < keywordMap.length; i++) {
                for (var j = 0; j < keywordMap[i].keys.length; j++) {
                    if (query.indexOf(keywordMap[i].keys[j]) >= 0) return keywordMap[i].hint;
                }
            }
            return fallback;
        }

        function escapeHtml(text) {
            return (text || '').replace(/[&<>"']/g, function (ch) {
                return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
            });
        }

        function renderAiResult(title, serviceText, advice, muted) {
            if (!aiResult) return;
            var safeAdvice = escapeHtml(advice || '').replace(/\n/g, '<br>');
            aiResult.innerHTML = '<div class="ai-orb">AI</div><h3>' + escapeHtml(title) + '</h3><p>判断为：<b>' + escapeHtml(serviceText) + '</b>。</p><p>' + safeAdvice + '</p><ol><li>保留用户原始问题</li><li>筛选同区域可接单工程师</li><li>' + (muted ? 'DeepSeek 未启用时使用本地规则' : '由 DeepSeek 生成诊断建议') + '</li></ol>';
        }

        function askDeepSeekText(serviceText, fallbackAdvice) {
            var queryText = (search.value || '').trim();
            if (!aiEndpoint || !queryText) return;
            var currentSeq = ++aiRequestSeq;
            renderAiResult('DeepSeek 正在诊断', serviceText, '我正在结合你的原始描述生成更自然的排查建议，请稍等。', false);
            fetch(aiEndpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: 'problem=' + encodeURIComponent(queryText) + '&serviceType=' + encodeURIComponent(serviceText)
            }).then(function (res) {
                return res.json();
            }).then(function (data) {
                if (currentSeq !== aiRequestSeq) return;
                if (data && data.ok) renderAiResult('DeepSeek 已完成诊断', serviceText, data.answer, false);
                else renderAiResult('AI 已完成初步定位', serviceText, fallbackAdvice || findHint(normalize(search.value)), true);
            }).catch(function () {
                if (currentSeq !== aiRequestSeq) return;
                renderAiResult('AI 已完成初步定位', serviceText, fallbackAdvice || findHint(normalize(search.value)), true);
            });
        }

        function askDeepSeek(card) {
            askDeepSeekText(card.dataset.deviceName + ' · ' + card.dataset.faultName, findHint(normalize(search.value)));
        }

        function selectCard(card, source) {
            issueCards.forEach(function (x) { x.classList.remove('active'); });
            card.classList.add('active');
            deviceSelect.value = card.dataset.deviceId;
            faultSelect.value = card.dataset.faultId;
            selectedIssue.textContent = card.dataset.deviceName + ' · ' + card.dataset.faultName;
            renderAiResult(source || '已匹配服务', card.dataset.deviceName + ' · ' + card.dataset.faultName, findHint(normalize(search.value)), true);
            askDeepSeek(card);
        }

        function runDiagnose() {
            var query = normalize(search.value);
            var best = null;
            var bestScore = -1;
            issueCards.forEach(function (card) {
                var score = scoreCard(card, query);
                card.hidden = query && score <= 0 && cardText(card).indexOf(query) < 0;
                if (score > bestScore) {
                    best = card;
                    bestScore = score;
                }
            });
            if (best && bestScore > 0) selectCard(best, 'AI 已完成初步定位');
            else if (issueCards.length) {
                issueCards.forEach(function (card) { card.hidden = false; });
                if (aiResult) {
                    aiResult.innerHTML = '<div class="ai-orb">AI</div><h3>需要再补充一点信息</h3><p>暂时没有非常明确的匹配。你可以描述设备名称、故障现象、报错提示，或直接从下方服务卡片中选择最接近的一项。</p><ol><li>例如“打印机卡纸”</li><li>例如“电脑蓝屏重启”</li><li>例如“冰箱冷藏不凉”</li></ol>';
                }
                askDeepSeekText('平台服务类型待确认', '我还没有匹配到明确服务类型，请补充设备名称、故障现象或报错信息。');
            }
        }

        issueCards.forEach(function (card) {
            card.addEventListener('click', function () { selectCard(card, '你已选择服务类型'); });
        });
        repairForm.querySelectorAll('[data-example]').forEach(function (button) {
            button.addEventListener('click', function () {
                search.value = button.dataset.example;
                runDiagnose();
            });
        });
        if (diagnoseButton) diagnoseButton.addEventListener('click', runDiagnose);
        if (search) search.addEventListener('input', runDiagnose);
        if (faultSelect && faultSelect.value) {
            var current = issueCards.filter(function (card) { return card.dataset.faultId === faultSelect.value; })[0];
            if (current) selectCard(current, '已恢复上次选择');
        }
        repairForm.addEventListener('submit', function (event) {
            if (!faultSelect.value && issueCards.length) runDiagnose();
            if (!faultSelect.value) {
                event.preventDefault();
                if (aiResult) {
                    aiResult.innerHTML = '<div class="ai-orb">AI</div><h3>还差一步</h3><p>请先搜索问题或点选一个可能服务，我才能帮你筛选合适的工程师。</p>';
                }
            }
            if (description && !description.value.trim() && search.value.trim()) {
                description.value = search.value.trim();
            }
        });
    }

})();
