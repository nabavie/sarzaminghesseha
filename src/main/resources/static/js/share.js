// Share button on the tale page: native share sheet on phones,
// copy-to-clipboard everywhere else.
(function () {
    'use strict';

    var button = document.getElementById('shareTale');
    if (!button) return;

    var url = button.getAttribute('data-share-url') || window.location.href;
    var title = button.getAttribute('data-share-title') || document.title;
    var originalText = button.textContent;

    function confirmCopied() {
        button.textContent = 'نشانی قصه کپی شد ✔';
        setTimeout(function () {
            button.textContent = originalText;
        }, 2500);
    }

    function copyFallback() {
        var field = document.createElement('input');
        field.value = url;
        field.setAttribute('readonly', 'readonly');
        field.style.position = 'fixed';
        field.style.opacity = '0';
        document.body.appendChild(field);
        field.select();
        try {
            document.execCommand('copy');
            confirmCopied();
        } catch (e) {
            window.prompt('نشانی این قصه را کپی کنید:', url);
        }
        document.body.removeChild(field);
    }

    button.addEventListener('click', function () {
        if (navigator.share) {
            navigator.share({ title: title, url: url }).catch(function () { /* dismissed */ });
            return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(url).then(confirmCopied).catch(copyFallback);
            return;
        }
        copyFallback();
    });
})();
