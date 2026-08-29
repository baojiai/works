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
    if (!repairForm) return;

    var search = repairForm.querySelector('[data-problem-search]');
    var diagnoseButton = repairForm.querySelector('[data-ai-diagnose]');
    var aiResult = repairForm.querySelector('[data-ai-result]');
    var selectedIssue = repairForm.querySelector('[data-selected-issue]');
    var issueGrid = repairForm.querySelector('.issue-card-grid');
    var issueCards = Array.prototype.slice.call(repairForm.querySelectorAll('[data-issue-card]'));
    var deviceSelect = repairForm.querySelector('select[name="deviceId"]');
    var faultSelect = repairForm.querySelector('select[name="faultId"]');
    var description = repairForm.querySelector('textarea[name="description"]');
    var aiEndpoint = repairForm.dataset.aiEndpoint;
    var aiRequestSeq = 0;
    var virtualCard = null;

    var ruleGroups = [
        {
            service: '计算机 · 无法开机',
            deviceWords: ['电脑', '主机', '笔记本', '显示器'],
            faultWords: ['开不了', '打不开', '不开机', '黑屏', '启动', '电源', '没反应', '无法开机'],
            hint: '建议先检查电源、插座、适配器和指示灯状态。若仍无反应，通常需要工程师检测电源模块或主板。'
        },
        {
            service: '计算机 · 系统异常',
            deviceWords: ['电脑', '笔记本', '系统'],
            faultWords: ['系统', '蓝屏', '卡顿', '死机', '报错', '崩溃', '中毒', '很慢', '重启'],
            hint: '这类问题可能来自系统文件、驱动或存储异常。建议提前记录报错提示，工程师可现场做系统诊断。'
        },
        {
            service: '打印设备 · 无法打印',
            deviceWords: ['打印机', '复印机', '打印'],
            faultWords: ['打印', '卡纸', '墨盒', '硒鼓', '不出纸', '脱机', '不能打印'],
            hint: '建议先确认纸张、墨盒/硒鼓、网络连接和打印队列。平台会优先推荐打印设备工程师。'
        },
        {
            service: '家用电器 · 制冷异常',
            deviceWords: ['冰箱', '冰柜', '空调', '冷柜'],
            faultWords: ['不制冷', '制冷', '冷冻', '冷藏', '温度', '结冰', '不凉'],
            hint: '制冷异常可能与压缩机、制冷剂、温控或风道有关，建议预约具备家电维修经验的工程师。'
        },
        {
            service: '家用电器 · 加热异常',
            deviceWords: ['微波炉', '烤箱', '电磁炉', '热水器', '电饭煲', '电水壶', '家电'],
            faultWords: ['不加热', '加热慢', '加热', '不热', '火力', '温度上不去', '食物不热'],
            hint: '微波炉或加热类家电常见原因包括磁控管、高压二极管、门锁开关、加热盘或温控件异常。'
        },
        {
            service: '家用电器 · 通电异常',
            deviceWords: ['微波炉', '烤箱', '电磁炉', '热水器', '电饭煲', '洗衣机', '家电'],
            faultWords: ['不通电', '跳闸', '插电没反应', '指示灯不亮', '烧保险', '电源'],
            hint: '通电异常通常需要检查电源线、保险、控制板或内部短路，不建议用户自行拆机。'
        },
        {
            service: '家用电器 · 异响或漏水',
            deviceWords: ['洗衣机', '空调', '热水器', '微波炉', '家电'],
            faultWords: ['异响', '漏水', '噪音', '震动', '嗡嗡响', '冒烟', '焦味'],
            hint: '异响、漏水或焦味属于需要尽快排查的异常，建议停止使用并预约家电工程师。'
        }
    ];

    function normalize(text) {
        return (text || '').toLowerCase().replace(/\s+/g, '');
    }

    function escapeHtml(text) {
        return (text || '').replace(/[&<>"']/g, function (ch) {
            return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
        });
    }

    function cardText(card) {
        return normalize([card.dataset.deviceName, card.dataset.faultName, card.textContent].join(' '));
    }

    function inferGroup(query) {
        var best = null;
        var bestScore = 0;
        ruleGroups.forEach(function (group) {
            var score = 0;
            group.deviceWords.forEach(function (word) {
                if (query.indexOf(normalize(word)) >= 0) score += 5;
            });
            group.faultWords.forEach(function (word) {
                if (query.indexOf(normalize(word)) >= 0) score += 6;
            });
            if (score > bestScore) {
                best = group;
                bestScore = score;
            }
        });
        return best ? { group: best, score: bestScore } : null;
    }

    function scoreCard(card, query) {
        var text = cardText(card);
        var score = 0;
        var inferred = inferGroup(query);
        if (!query) return 0;
        if (text.indexOf(query) >= 0) score += 10;
        if (inferred) {
            var parts = inferred.group.service.split(' · ');
            if (text.indexOf(normalize(parts[0])) >= 0) score += 5;
            if (text.indexOf(normalize(parts[1])) >= 0) score += 9;
        }
        ruleGroups.forEach(function (group) {
            group.deviceWords.concat(group.faultWords).forEach(function (word) {
                word = normalize(word);
                if (query.indexOf(word) >= 0 && text.indexOf(word) >= 0) score += 4;
            });
        });
        return score;
    }

    function findHint(query) {
        var inferred = inferGroup(query);
        return inferred ? inferred.group.hint : '我会先根据你的描述进行初步判断；如果平台服务目录里暂无对应类目，会提醒管理员补充服务类型。';
    }

    function renderAiResult(title, serviceText, advice, muted) {
        if (!aiResult) return;
        var safeAdvice = escapeHtml(advice || '').replace(/\n/g, '<br>');
        aiResult.innerHTML = '<div class="ai-orb">AI</div><h3>' + escapeHtml(title) + '</h3><p>判断为：<b>' + escapeHtml(serviceText) + '</b>。</p><p>' + safeAdvice + '</p><ol><li>保留用户原始问题</li><li>筛选同区域可接单工程师</li><li>' + (muted ? '平台本地规则辅助匹配' : '由 DeepSeek 生成诊断建议') + '</li></ol>';
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

    function refreshIssueCards() {
        issueCards = Array.prototype.slice.call(repairForm.querySelectorAll('[data-issue-card]'));
    }

    function selectCard(card, source) {
        refreshIssueCards();
        issueCards.forEach(function (x) { x.classList.remove('active'); });
        card.classList.add('active');
        if (card.dataset.deviceId && card.dataset.faultId) {
            deviceSelect.value = card.dataset.deviceId;
            faultSelect.value = card.dataset.faultId;
            selectedIssue.textContent = card.dataset.deviceName + ' · ' + card.dataset.faultName;
        } else {
            deviceSelect.value = '';
            faultSelect.value = '';
            selectedIssue.textContent = card.dataset.deviceName + ' · 平台待补充';
        }
        var serviceText = card.dataset.deviceName + ' · ' + card.dataset.faultName;
        renderAiResult(source || '已匹配服务', serviceText, findHint(normalize(search.value)), true);
        askDeepSeekText(serviceText, findHint(normalize(search.value)));
    }

    function createVirtualCard(inferred) {
        if (!issueGrid || !inferred) return null;
        if (!virtualCard) {
            virtualCard = document.createElement('button');
            virtualCard.type = 'button';
            virtualCard.className = 'issue-card ai-generated-service';
            virtualCard.setAttribute('data-issue-card', 'true');
            virtualCard.addEventListener('click', function () { selectCard(virtualCard, 'AI 推荐的新增服务'); });
            issueGrid.insertBefore(virtualCard, issueGrid.firstChild);
        }
        var parts = inferred.group.service.split(' · ');
        virtualCard.dataset.deviceName = parts[0];
        virtualCard.dataset.faultName = parts[1];
        virtualCard.dataset.deviceId = '';
        virtualCard.dataset.faultId = '';
        virtualCard.innerHTML = '<span class="issue-icon">✨</span><b>' + escapeHtml(parts[1]) + '</b><small>' + escapeHtml(parts[0]) + ' · AI 根据你的问题动态推荐</small><em>平台服务目录待确认</em>';
        virtualCard.hidden = false;
        refreshIssueCards();
        return virtualCard;
    }

    function findPlatformCardFor(inferred) {
        if (!inferred) return null;
        var parts = inferred.group.service.split(' · ');
        var deviceName = normalize(parts[0]);
        var faultName = normalize(parts[1]);
        return issueCards.filter(function (card) {
            return normalize(card.dataset.deviceName).indexOf(deviceName) >= 0 && normalize(card.dataset.faultName).indexOf(faultName) >= 0;
        })[0] || null;
    }

    function runDiagnose() {
        var query = normalize(search.value);
        var inferred = inferGroup(query);
        var platformCard = findPlatformCardFor(inferred);
        var best = null;
        var bestScore = -1;

        if (virtualCard) virtualCard.hidden = true;
        issueCards.forEach(function (card) {
            if (card === virtualCard) return;
            var score = scoreCard(card, query);
            card.hidden = !!query && score <= 0;
            if (score > bestScore) {
                best = card;
                bestScore = score;
            }
        });

        if (platformCard) {
            issueCards.forEach(function (card) { if (card !== platformCard && card !== virtualCard) card.hidden = true; });
            platformCard.hidden = false;
            selectCard(platformCard, 'AI 已匹配平台服务');
        } else if (inferred) {
            var generated = createVirtualCard(inferred);
            issueCards.forEach(function (card) { if (card !== generated) card.hidden = true; });
            selectCard(generated, 'AI 识别到新服务需求');
        } else if (best && bestScore > 0) {
            selectCard(best, 'AI 已完成初步定位');
        } else {
            issueCards.forEach(function (card) { card.hidden = false; });
            selectedIssue.textContent = '平台服务类型待确认';
            askDeepSeekText('平台服务类型待确认', '我还没有匹配到明确服务类型，请补充设备名称、故障现象或报错信息。');
        }
    }

    refreshIssueCards();
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
            renderAiResult('还差一步', selectedIssue.textContent || '平台服务类型待确认', '当前问题还没有对应的可预约平台服务。请先选择一个平台已有服务，或联系管理员在基础数据中新增服务类目。', true);
        }
        if (description && !description.value.trim() && search.value.trim()) {
            description.value = search.value.trim();
        }
    });
})();
