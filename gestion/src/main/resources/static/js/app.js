(function () {
    var body = document.body;
    var toggle = document.getElementById('sidebar-toggle');
    var sidebar = document.getElementById('sidebar');
    var backdrop = document.querySelector('.sidebar-backdrop');

    function fermerSidebar() {
        body.classList.remove('sidebar-open');
        if (toggle) {
            toggle.setAttribute('aria-expanded', 'false');
        }
    }

    if (toggle) {
        toggle.addEventListener('click', function () {
            var ouvert = body.classList.toggle('sidebar-open');
            toggle.setAttribute('aria-expanded', ouvert ? 'true' : 'false');
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', fermerSidebar);
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            fermerSidebar();
        }
    });

    if (sidebar) {
        sidebar.querySelectorAll('a').forEach(function (lien) {
            lien.addEventListener('click', function () {
                if (window.innerWidth <= 960) {
                    fermerSidebar();
                }
            });
        });
    }
})();
