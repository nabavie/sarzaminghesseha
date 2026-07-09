// Audio player enhancements for the tale detail page:
// resumes from the saved position and reports listening progress
// for logged-in users via POST /api/progress.
(function () {
    'use strict';

    var player = document.getElementById('talePlayer');
    if (!player) return;

    var taleId = parseInt(player.dataset.taleId, 10);
    var resume = parseInt(player.dataset.resumeSeconds || '0', 10);
    var authenticated = player.dataset.authenticated === 'true';

    if (resume > 0) {
        player.addEventListener('loadedmetadata', function () {
            if (isFinite(player.duration) && resume < player.duration - 5) {
                player.currentTime = resume;
            }
        });
    }

    if (!authenticated || !taleId) return;

    var csrfMeta = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    var lastSent = -1;

    function send(finished) {
        var seconds = Math.floor(player.currentTime || 0);
        if (!finished && seconds === lastSent) return;
        lastSent = seconds;

        var headers = { 'Content-Type': 'application/json' };
        if (csrfMeta && csrfHeaderMeta) {
            headers[csrfHeaderMeta.content] = csrfMeta.content;
        }
        var duration = isFinite(player.duration) ? Math.floor(player.duration) : null;
        fetch('/api/progress', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                taleId: taleId,
                seconds: finished ? 0 : seconds,
                duration: duration,
                finished: !!finished
            })
        }).catch(function () { /* progress is best-effort */ });
    }

    var lastTick = 0;
    player.addEventListener('timeupdate', function () {
        var now = Date.now();
        if (now - lastTick >= 10000) { // every 10 seconds while playing
            lastTick = now;
            send(false);
        }
    });
    player.addEventListener('pause', function () { send(false); });
    player.addEventListener('ended', function () { send(true); });
})();
