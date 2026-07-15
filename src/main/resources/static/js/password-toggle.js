(function () {
    'use strict';

    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        var input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;

        button.addEventListener('click', function () {
            var showPassword = input.type === 'password';
            input.type = showPassword ? 'text' : 'password';
            button.querySelector('.password-toggle-show').classList.toggle('d-none', showPassword);
            button.querySelector('.password-toggle-hide').classList.toggle('d-none', !showPassword);
            button.setAttribute('aria-label', showPassword ? 'پنهان کردن رمز عبور' : 'نمایش رمز عبور');
            button.setAttribute('title', showPassword ? 'پنهان کردن رمز عبور' : 'نمایش رمز عبور');
            button.setAttribute('aria-pressed', String(showPassword));
            input.focus();
        });
    });
})();
