/**
 * Warns about oversized files the moment they are picked, instead of after the user
 * has waited through a doomed upload. Applies to any file input carrying
 * data-max-bytes plus data-max-label (the limit, already in Persian digits) and
 * data-kind ("audio" or "image").
 */
(function () {
    const PERSIAN_DIGITS = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];

    function persianDigits(text) {
        return String(text).replace(/[0-9]/g, function (d) {
            return PERSIAN_DIGITS[Number(d)];
        });
    }

    function megabytes(bytes) {
        let text = (bytes / (1024 * 1024)).toFixed(1);
        if (text.endsWith('.0')) {
            text = text.slice(0, -2);
        }
        return persianDigits(text);
    }

    function message(input, file) {
        const limit = input.getAttribute('data-max-label') || '';
        const actual = megabytes(file.size);
        if (input.getAttribute('data-kind') === 'audio') {
            return 'حجم فایل صوتی شما ' + actual + ' مگابایت است؛ حداکثر مجاز ' + limit
                + ' مگابایت است. لطفاً قصه را کوتاه‌تر ضبط کنید یا همان فایل را با کیفیت پایین‌تر'
                + ' (mp3 سبک‌تر) بفرستید.';
        }
        return 'حجم عکس شما ' + actual + ' مگابایت است؛ حداکثر مجاز ' + limit
            + ' مگابایت است. لطفاً عکس را کوچک‌تر کنید یا با کیفیت کمتر ذخیره کنید.';
    }

    function errorBox(input) {
        let box = input.parentElement.querySelector('.file-size-error');
        if (!box) {
            box = document.createElement('div');
            box.className = 'file-size-error alert alert-danger py-2 mt-2 mb-0';
            box.setAttribute('role', 'alert');
            input.parentElement.appendChild(box);
        }
        return box;
    }

    document.querySelectorAll('input[type="file"][data-max-bytes]').forEach(function (input) {
        const max = Number(input.getAttribute('data-max-bytes'));
        if (!max) return;

        input.addEventListener('change', function () {
            const box = errorBox(input);
            const file = input.files && input.files[0];
            if (!file || file.size <= max) {
                box.classList.add('d-none');
                box.textContent = '';
                input.classList.remove('is-invalid');
                return;
            }
            box.textContent = message(input, file);
            box.classList.remove('d-none');
            input.classList.add('is-invalid');
            // clearing keeps the form from posting a file we already know is rejected
            input.value = '';
        });
    });
})();
