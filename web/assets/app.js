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
        function filterFaults(resetSelection) {
            Array.prototype.forEach.call(fault.options, function (option) {
                option.hidden = !!option.dataset.device && option.dataset.device !== device.value;
            });
            if (resetSelection) fault.value = '';
        }
        device.addEventListener('change', function () { filterFaults(true); });
        filterFaults(false);
    }
})();
