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
    var issueGrid = repairForm.querySelector('[data-ai-service-grid]') || repairForm.querySelector('.issue-card-grid');
    var serviceEmpty = repairForm.querySelector('[data-ai-service-empty]');
    var defaultServiceEmptyHtml = serviceEmpty ? serviceEmpty.innerHTML : '';
    var platformCards = Array.prototype.slice.call(repairForm.querySelectorAll('[data-issue-card]'));
    var deviceSelect = repairForm.querySelector('select[name="deviceId"]');
    var faultSelect = repairForm.querySelector('select[name="faultId"]');
    var description = repairForm.querySelector('textarea[name="description"]');
    var aiEndpoint = repairForm.dataset.aiEndpoint;
    var aiRequestSeq = 0;
    var generatedCards = [];

    platformCards.forEach(function (card) {
        card.classList.add('platform-catalog-card');
        card.hidden = true;
    });

    var localRules = [
        { title: '电脑开机检测', device: '计算机', fault: '无法开机', note: '适合黑屏、按电源无反应、主机无法启动。', words: ['电脑', '主机', '笔记本', '开不了', '打不开', '无法开机', '黑屏', '电源'] },
        { title: '系统故障排查', device: '计算机', fault: '系统异常', note: '适合蓝屏、卡顿、频繁重启、系统报错。', words: ['电脑', '系统', '蓝屏', '卡顿', '死机', '报错', '重启'] },
        { title: '打印设备维修', device: '打印设备', fault: '无法打印', note: '适合卡纸、脱机、不出纸、打印失败。', words: ['打印机', '打印', '卡纸', '脱机', '墨盒', '硒鼓'] },
        { title: '微波炉加热维修', device: '家用电器', fault: '加热异常', note: '适合微波炉不加热、加热慢、食物不热。', words: ['微波炉', '不加热', '加热', '不热', '食物'] },
        { title: '电饭煲通电检测', device: '家用电器', fault: '通电异常', note: '适合电饭煲、热水器、小家电插电无反应。', words: ['电饭煲', '热水器', '电磁炉', '打不开', '不通电', '跳闸', '指示灯'] },
        { title: '家电异响漏水维修', device: '家用电器', fault: '异响或漏水', note: '适合洗衣机、空调、热水器异响、漏水、焦味。', words: ['洗衣机', '异响', '漏水', '噪音', '震动', '焦味', '冒烟', '脱水', '甩干'] },
        { title: '制冷系统检修', device: '家用电器', fault: '制冷异常', note: '适合冰箱、空调不制冷、冷藏不凉。', words: ['冰箱', '空调', '不制冷', '制冷', '冷藏', '冷冻'] }
    ];

    function normalize(text) {
        return (text || '').toLowerCase().replace(/\s+/g, '');
    }

    function escapeHtml(text) {
        return (text || '').replace(/[&<>"']/g, function (ch) {
            return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
        });
    }

    function platformMatch(deviceName, faultName) {
        var d = normalize(deviceName);
        var f = normalize(faultName);
        return platformCards.filter(function (card) {
            return normalize(card.dataset.deviceName).indexOf(d) >= 0 && normalize(card.dataset.faultName).indexOf(f) >= 0;
        })[0] || null;
    }

    function localSuggestions(query) {
        var q = normalize(query);
        var ranked = localRules.map(function (rule) {
            var score = 0;
            rule.words.forEach(function (word) {
                if (q.indexOf(normalize(word)) >= 0) score += 1;
            });
            return { rule: rule, score: score };
        }).sort(function (a, b) { return b.score - a.score; });
        return ranked.filter(function (x) { return x.score > 0; }).slice(0, 4).map(function (x) { return x.rule; });
    }

    function hasRepairSignal(query) {
        var q = normalize(query);
        if (q.length < 3) return false;
        var deviceWords = ['电脑', '主机', '笔记本', '打印机', '打印', '微波炉', '电饭煲', '热水器', '电磁炉', '洗衣机', '冰箱', '空调', '家电', '设备'];
        var symptomWords = ['不开机', '无法开机', '打不开', '坏', '故障', '异常', '不加热', '不热', '不制冷', '卡纸', '蓝屏', '卡顿', '死机', '漏水', '异响', '噪音', '冒烟', '焦味', '跳闸', '不通电', '没反应', '脱水', '甩干'];
        return deviceWords.some(function (word) { return q.indexOf(normalize(word)) >= 0; })
            && symptomWords.some(function (word) { return q.indexOf(normalize(word)) >= 0; });
    }

    function renderAiResult(title, serviceText, advice, source) {
        if (!aiResult) return;
        var safeAdvice = escapeHtml(advice || '').replace(/\n/g, '<br>');
        aiResult.innerHTML = '<div class="ai-orb">AI</div><h3>' + escapeHtml(title) + '</h3><p>判断为：<b>' + escapeHtml(serviceText) + '</b>。</p><p>' + safeAdvice + '</p><ol><li>根据输入生成服务词条</li><li>自动匹配平台可预约分类</li><li>' + escapeHtml(source || 'AI 辅助推荐') + '</li></ol>';
    }

    function clearGenerated() {
        generatedCards.forEach(function (card) { card.remove(); });
        generatedCards = [];
        if (serviceEmpty) {
            serviceEmpty.innerHTML = defaultServiceEmptyHtml;
            serviceEmpty.hidden = false;
        }
    }

    function renderNoConfidentMatch(message) {
        clearGenerated();
        deviceSelect.value = '';
        faultSelect.value = '';
        selectedIssue.textContent = '暂不推荐';
        if (serviceEmpty) {
            serviceEmpty.hidden = false;
            serviceEmpty.innerHTML = '<span>?</span><b>暂时无法生成可靠推荐</b><small>' + escapeHtml(message || '请补充设备名称和具体现象，例如“微波炉不加热”“打印机一直卡纸”“电脑蓝屏重启”。') + '</small>';
        }
    }

    function iconFor(deviceName, faultName) {
        var text = deviceName + faultName;
        if (text.indexOf('打印') >= 0) return '🖨';
        if (text.indexOf('计算机') >= 0 || text.indexOf('电脑') >= 0) return '💻';
        if (text.indexOf('加热') >= 0 || text.indexOf('微波') >= 0 || text.indexOf('电饭煲') >= 0) return '🔥';
        if (text.indexOf('制冷') >= 0 || text.indexOf('冰箱') >= 0 || text.indexOf('空调') >= 0) return '❄';
        if (text.indexOf('漏水') >= 0) return '💧';
        return '✨';
    }

    function renderSuggestions(suggestions, source) {
        clearGenerated();
        if (serviceEmpty) serviceEmpty.hidden = true;
        platformCards.forEach(function (card) { card.hidden = true; card.classList.remove('active'); });
        suggestions.slice(0, 4).forEach(function (item, index) {
            var matched = platformMatch(item.device, item.fault);
            var card = document.createElement('button');
            card.type = 'button';
            card.className = 'issue-card ai-generated-service' + (matched ? '' : ' unavailable');
            card.dataset.deviceName = item.device || '服务类型';
            card.dataset.faultName = item.fault || item.title || '待确认';
            card.dataset.deviceId = matched ? matched.dataset.deviceId : '';
            card.dataset.faultId = matched ? matched.dataset.faultId : '';
            card.innerHTML =
                '<span class="issue-icon">' + iconFor(item.device || '', item.title || item.fault || '') + '</span>' +
                '<b>' + escapeHtml(item.title || item.fault || 'AI 推荐服务') + '</b>' +
                '<small>' + escapeHtml(item.note || ((item.device || '') + ' · ' + (item.fault || ''))) + '</small>' +
                '<em>' + (matched ? '可预约工程师' : '平台待补充类目') + '</em>';
            card.addEventListener('click', function () {
                generatedCards.forEach(function (x) { x.classList.remove('active'); });
                card.classList.add('active');
                deviceSelect.value = card.dataset.deviceId || '';
                faultSelect.value = card.dataset.faultId || '';
                selectedIssue.textContent = (item.title || item.fault || 'AI 推荐服务') + (matched ? ' · 可预约' : ' · 待补充');
                renderAiResult(source || 'AI 已生成服务词条', (item.device || '') + ' · ' + (item.fault || ''), item.note || '', matched ? '该词条已映射到平台服务目录' : '该词条暂未映射到平台目录');
            });
            generatedCards.push(card);
            issueGrid.appendChild(card);
            if (index === 0) card.click();
        });
    }

    function askDeepSeek(serviceText, fallbackSuggestions) {
        var queryText = (search.value || '').trim();
        if (!aiEndpoint || !queryText) return;
        var currentSeq = ++aiRequestSeq;
        renderAiResult('DeepSeek 正在生成词条', serviceText, '我正在根据你的问题生成更贴近场景的服务词条。', '等待 DeepSeek 返回');
        fetch(aiEndpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: 'problem=' + encodeURIComponent(queryText) + '&serviceType=' + encodeURIComponent(serviceText)
        }).then(function (res) {
            return res.json();
        }).then(function (data) {
            if (currentSeq !== aiRequestSeq) return;
            if (data && data.ok && data.suggestions && data.suggestions.length) {
                renderSuggestions(data.suggestions, 'DeepSeek 生成');
                renderAiResult('DeepSeek 已生成词条', serviceText, data.answer, '由 DeepSeek 生成服务建议');
            } else if (fallbackSuggestions && fallbackSuggestions.length) {
                renderSuggestions(fallbackSuggestions, '本地 AI 生成');
            } else {
                renderNoConfidentMatch('AI 没有给出足够可靠的服务词条。请补充设备名称、故障现象和发生场景后再诊断。');
            }
        }).catch(function () {
            if (currentSeq !== aiRequestSeq) return;
            if (fallbackSuggestions && fallbackSuggestions.length) {
                renderSuggestions(fallbackSuggestions, '本地 AI 生成');
            } else {
                renderNoConfidentMatch('当前无法获得可靠 AI 结果，也没有本地规则命中。请补充更明确的维修信息。');
            }
        });
    }

    function runDiagnose() {
        var queryText = (search.value || '').trim();
        if (!queryText) {
            clearGenerated();
            selectedIssue.textContent = '等待输入问题';
            renderAiResult('等待你的问题', '暂未判断', '输入问题后，AI 会生成与问题相关的服务词条。', 'AI 生成服务词条');
            return;
        }
        if (!hasRepairSignal(queryText)) {
            renderNoConfidentMatch('这个描述缺少明确的设备名称或故障现象，系统不会强行推荐。请写清楚“什么设备 + 出现什么问题”。');
            renderAiResult('无法可靠判断', '信息不足', '为了避免误导，当前不会生成服务词条。请补充设备名称和具体现象后再诊断。', '实事求是模式');
            return;
        }
        var suggestions = localSuggestions(queryText);
        if (!suggestions.length) {
            renderNoConfidentMatch('目前平台本地规则没有匹配到这个问题。我会尝试请求 DeepSeek；如果 AI 也不能明确判断，就不会给出预约词条。');
            askDeepSeek('待进一步判断', []);
            return;
        }
        renderSuggestions(suggestions, '本地 AI 快速生成');
        askDeepSeek(suggestions[0].device + ' · ' + suggestions[0].fault, suggestions);
    }

    repairForm.querySelectorAll('[data-example]').forEach(function (button) {
        button.addEventListener('click', function () {
            search.value = button.dataset.example;
            runDiagnose();
        });
    });
    if (diagnoseButton) diagnoseButton.addEventListener('click', runDiagnose);
    if (search) {
        search.addEventListener('input', function () {
            clearGenerated();
            selectedIssue.textContent = '等待 AI 诊断';
            deviceSelect.value = '';
            faultSelect.value = '';
            renderAiResult('等待诊断', '暂未判断', '问题已更新，请点击“AI 诊断”生成新的服务词条。', 'AI 生成服务词条');
        });
    }

    repairForm.addEventListener('submit', function (event) {
        if (!faultSelect.value) {
            event.preventDefault();
            runDiagnose();
            renderAiResult('还差一步', selectedIssue.textContent || '服务类型待确认', '请先从 AI 生成的词条中选择一个“可预约工程师”的服务；如果显示“平台待补充类目”，需要管理员先维护服务目录和工程师技能。', '预约需要平台服务映射');
        }
        if (description && !description.value.trim() && search.value.trim()) {
            description.value = search.value.trim();
        }
    });

    renderAiResult('等待你的问题', '暂未判断', '输入问题后，AI 会生成与问题相关的服务词条，而不是展示固定选项。', 'AI 生成服务词条');
})();
