/**
 * Progressive audio upload for the tale form.
 * Uploads the audio file first (with a progress bar) so slow connections
 * don't hit gateway timeouts on the full multipart form POST.
 */
(function () {
    const form = document.getElementById('taleForm');
    if (!form) return;

    const audioInput = document.getElementById('audio');
    const audioFilename = document.getElementById('audioFilename');
    const audioContentType = document.getElementById('audioContentType');
    const progressWrap = document.getElementById('uploadProgressWrap');
    const progressBar = document.getElementById('uploadProgressBar');
    const progressLabel = document.getElementById('uploadProgressLabel');
    const uploadError = document.getElementById('uploadError');
    const submitBtn = form.querySelector('button[type="submit"]');
    const uploadUrl = form.getAttribute('data-upload-url');

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        const headers = {};
        if (token && header) {
            headers[header.getAttribute('content')] = token.getAttribute('content');
        }
        return headers;
    }

    function showProgress(pct, label) {
        if (progressWrap) progressWrap.classList.remove('d-none');
        if (progressBar) {
            progressBar.style.width = pct + '%';
            progressBar.setAttribute('aria-valuenow', String(pct));
        }
        if (progressLabel) progressLabel.textContent = label;
    }

    function hideProgress() {
        if (progressWrap) progressWrap.classList.add('d-none');
    }

    function showError(msg) {
        if (!uploadError) return;
        uploadError.textContent = msg;
        uploadError.classList.remove('d-none');
    }

    function clearError() {
        if (!uploadError) return;
        uploadError.classList.add('d-none');
        uploadError.textContent = '';
    }

    function uploadFile(file) {
        return new Promise(function (resolve, reject) {
            const xhr = new XMLHttpRequest();
            const data = new FormData();
            data.append('audio', file, file.name || 'recording.webm');

            xhr.open('POST', uploadUrl, true);
            const headers = csrfHeaders();
            Object.keys(headers).forEach(function (k) {
                xhr.setRequestHeader(k, headers[k]);
            });

            xhr.upload.onprogress = function (e) {
                if (!e.lengthComputable) {
                    showProgress(30, 'در حال آپلود صدا…');
                    return;
                }
                const pct = Math.max(1, Math.min(99, Math.round((e.loaded / e.total) * 100)));
                const loadedMb = (e.loaded / (1024 * 1024)).toFixed(1);
                const totalMb = (e.total / (1024 * 1024)).toFixed(1);
                showProgress(pct, 'آپلود صدا: ' + pct + '٪ (' + loadedMb + ' از ' + totalMb + ' مگابایت)');
            };

            xhr.onload = function () {
                let body = {};
                try {
                    body = JSON.parse(xhr.responseText || '{}');
                } catch (ignore) {
                    body = {};
                }
                if (xhr.status >= 200 && xhr.status < 300 && body.filename) {
                    showProgress(100, 'آپلود صدا تمام شد ✔');
                    resolve(body);
                } else {
                    reject(new Error(body.error || 'آپلود ناموفق بود'));
                }
            };

            xhr.onerror = function () {
                reject(new Error('اتصال قطع شد؛ اینترنت را بررسی کنید و دوباره بفرستید'));
            };

            xhr.ontimeout = function () {
                reject(new Error('آپلود خیلی طول کشید؛ لطفاً دوباره تلاش کنید'));
            };

            xhr.timeout = 15 * 60 * 1000; // 15 minutes
            xhr.send(data);
        });
    }

    if (audioInput) {
        audioInput.addEventListener('change', function () {
            if (audioFilename) audioFilename.value = '';
            if (audioContentType) audioContentType.value = '';
            clearError();
            hideProgress();
        });
    }

    form.addEventListener('submit', function (e) {
        clearError();
        const hasPreUploaded = audioFilename && audioFilename.value;
        const file = audioInput && audioInput.files && audioInput.files[0];

        // Edit without changing audio, or already uploaded this session
        if (!file) {
            return;
        }

        // Progressive upload path
        e.preventDefault();
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.dataset.originalText = submitBtn.textContent;
            submitBtn.textContent = 'در حال آپلود صدا…';
        }

        uploadFile(file).then(function (body) {
            audioFilename.value = body.filename;
            if (audioContentType) {
                audioContentType.value = body.contentType || file.type || 'audio/mpeg';
            }
            // Avoid re-sending the large blob with the form
            try {
                audioInput.value = '';
            } catch (ignore) { /* ignore */ }
            if (submitBtn) {
                submitBtn.textContent = 'در حال ثبت قصه…';
            }
            form.submit();
        }).catch(function (err) {
            showError(err.message || 'آپلود ناموفق بود');
            hideProgress();
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = submitBtn.dataset.originalText || 'فرستادن قصه';
            }
        });
    });
})();
