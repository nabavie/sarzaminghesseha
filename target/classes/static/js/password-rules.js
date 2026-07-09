// Live password-rule checklist: any input with data-password-rules shows
// which requirements (length >= 8, a letter, a digit) currently pass,
// inside the element referenced by data-password-rules.
(function () {
    'use strict';

    var RULES = [
        { key: 'length', test: function (v) { return v.length >= 8; } },
        { key: 'letter', test: function (v) { return /\p{L}/u.test(v); } },
        { key: 'digit', test: function (v) { return /\d/.test(v); } }
    ];

    document.querySelectorAll('input[data-password-rules]').forEach(function (input) {
        var box = document.getElementById(input.dataset.passwordRules);
        if (!box) return;

        function update() {
            var value = input.value || '';
            RULES.forEach(function (rule) {
                var item = box.querySelector('[data-rule="' + rule.key + '"]');
                if (!item) return;
                var ok = rule.test(value);
                item.classList.toggle('text-success', ok);
                item.classList.toggle('text-muted', !ok);
                var mark = item.querySelector('.rule-mark');
                if (mark) mark.textContent = ok ? '✔' : '✖';
            });
        }

        input.addEventListener('input', update);
        update();
    });
})();
