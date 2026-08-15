// Share button on the tale page: native share sheet on phones,
// copy-to-clipboard everywhere else.
// Uses a delegated listener because auto-next replaces the button's
// section with fresh server-rendered HTML (see player.js).
(function () {
    'use strict';

    function confirmCopied(button, originalText) {
        button.textContent = 'نشانی قصه کپی شد ✔';
        setTimeout(function () {
            button.textContent = originalText;
        }, 2500);
    }

    function copyFallback(button, originalText, url) {
        var field = document.createElement('input');
        field.value = url;
        field.setAttribute('readonly', 'readonly');
        field.style.position = 'fixed';
        field.style.opacity = '0';
        document.body.appendChild(field);
        field.select();
        try {
            document.execCommand('copy');
            confirmCopied(button, originalText);
        } catch (e) {
            window.prompt('نشانی این قصه را کپی کنید:', url);
        }
        document.body.removeChild(field);
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest ? event.target.closest('#shareTale') : null;
        if (!button) return;

        var url = button.getAttribute('data-share-url') || window.location.href;
        var title = button.getAttribute('data-share-title') || document.title;
        var originalText = button.textContent;
        if (navigator.share) {
            navigator.share({ title: title, url: url }).catch(function () { /* dismissed */ });
            return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(url)
                .then(function () { confirmCopied(button, originalText); })
                .catch(function () { copyFallback(button, originalText, url); });
            return;
        }
        copyFallback(button, originalText, url);
    });
})();
