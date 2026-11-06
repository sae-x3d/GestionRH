(function () {
    document.querySelectorAll('[data-password-toggle]').forEach(function (bouton) {
        var champ = document.getElementById(bouton.getAttribute('data-password-toggle'));
        if (!champ) {
            return;
        }

        var iconeAfficher = bouton.querySelector('.password-toggle-show');
        var iconeMasquer = bouton.querySelector('.password-toggle-hide');

        function afficherMotDePasse(affiche) {
            champ.type = affiche ? 'text' : 'password';
            bouton.setAttribute('aria-pressed', affiche ? 'true' : 'false');
            bouton.setAttribute('aria-label', affiche ? 'Masquer le mot de passe' : 'Afficher le mot de passe');

            if (iconeAfficher) {
                iconeAfficher.hidden = affiche;
            }
            if (iconeMasquer) {
                iconeMasquer.hidden = !affiche;
            }
        }

        bouton.addEventListener('click', function () {
            afficherMotDePasse(champ.type === 'password');
            champ.focus();
        });

        afficherMotDePasse(false);
    });
})();
