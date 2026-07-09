// In-browser voice recording for the storyteller form.
// Records via MediaRecorder and injects the result into the form's file input,
// so the normal multipart submit carries the recorded audio.
(function () {
    'use strict';

    var recordBtn = document.getElementById('recordBtn');
    if (!recordBtn) return;

    var timerEl = document.getElementById('recordTimer');
    var statusEl = document.getElementById('recordStatus');
    var previewWrap = document.getElementById('previewWrap');
    var previewAudio = document.getElementById('previewAudio');
    var errorEl = document.getElementById('recordError');
    var fileInput = document.getElementById('audio');

    var mediaRecorder = null;
    var chunks = [];
    var timerInterval = null;
    var seconds = 0;

    var PERSIAN_DIGITS = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];

    function faDigits(text) {
        return String(text).replace(/[0-9]/g, function (d) { return PERSIAN_DIGITS[+d]; });
    }

    function formatTime(total) {
        var m = Math.floor(total / 60);
        var s = total % 60;
        return faDigits(m + ':' + (s < 10 ? '0' : '') + s);
    }

    function showError(message) {
        errorEl.textContent = message;
        errorEl.classList.remove('d-none');
    }

    function pickMimeType() {
        var candidates = ['audio/webm', 'audio/mp4', 'audio/ogg'];
        for (var i = 0; i < candidates.length; i++) {
            if (window.MediaRecorder && MediaRecorder.isTypeSupported(candidates[i])) {
                return candidates[i];
            }
        }
        return '';
    }

    function startRecording() {
        errorEl.classList.add('d-none');
        if (!navigator.mediaDevices || !window.MediaRecorder) {
            showError('مرورگر شما از ضبط صدا پشتیبانی نمی‌کند؛ لطفاً فایل صوتی را از تب «انتخاب فایل» بفرستید.');
            return;
        }
        navigator.mediaDevices.getUserMedia({ audio: true }).then(function (stream) {
            var mimeType = pickMimeType();
            mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType: mimeType }) : new MediaRecorder(stream);
            chunks = [];
            mediaRecorder.ondataavailable = function (e) {
                if (e.data && e.data.size > 0) chunks.push(e.data);
            };
            mediaRecorder.onstop = function () {
                stream.getTracks().forEach(function (t) { t.stop(); });
                finishRecording();
            };
            mediaRecorder.start();

            seconds = 0;
            timerEl.textContent = formatTime(0);
            timerInterval = setInterval(function () {
                seconds++;
                timerEl.textContent = formatTime(seconds);
            }, 1000);

            recordBtn.classList.add('recording');
            recordBtn.textContent = '⏹️';
            statusEl.textContent = 'در حال ضبط... دوباره دکمه را بزنید تا تمام شود';
        }).catch(function () {
            showError('دسترسی به میکروفون داده نشد. لطفاً اجازهٔ استفاده از میکروفون را بدهید.');
        });
    }

    function stopRecording() {
        if (mediaRecorder && mediaRecorder.state !== 'inactive') {
            mediaRecorder.stop();
        }
        clearInterval(timerInterval);
        recordBtn.classList.remove('recording');
        recordBtn.textContent = '🎙️';
    }

    function finishRecording() {
        var type = (mediaRecorder && mediaRecorder.mimeType) || 'audio/webm';
        var blob = new Blob(chunks, { type: type });
        var extension = type.indexOf('mp4') !== -1 ? 'm4a' : type.indexOf('ogg') !== -1 ? 'ogg' : 'webm';
        var file = new File([blob], 'recording.' + extension, { type: type });

        var dt = new DataTransfer();
        dt.items.add(file);
        fileInput.files = dt.files;

        previewAudio.src = URL.createObjectURL(blob);
        previewWrap.classList.remove('d-none');
        statusEl.textContent = 'ضبط تمام شد ✔';
    }

    recordBtn.addEventListener('click', function () {
        if (mediaRecorder && mediaRecorder.state === 'recording') {
            stopRecording();
        } else {
            startRecording();
        }
    });
})();
