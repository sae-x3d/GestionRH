(function () {
    var conteneur = document.getElementById('lignes-parcours');
    var gabarit = document.getElementById('gabarit-ligne');
    var boutonAjouter = document.getElementById('ajouter-ligne');

    function prochainIndex() {
        return conteneur.querySelectorAll('.ligne-parcours').length;
    }

    function renumeroter() {
        conteneur.querySelectorAll('.ligne-parcours').forEach(function (ligne, index) {
            ligne.querySelectorAll('[name]').forEach(function (champ) {
                champ.name = champ.name.replace(/parcours\[\d+]/, 'parcours[' + index + ']');
                champ.id = champ.id ? champ.id.replace(/parcours\[\d+]/, 'parcours[' + index + ']') : champ.id;
            });
        });
    }

    function brancherRetrait(ligne) {
        var bouton = ligne.querySelector('.retirer-ligne');
        if (bouton) {
            bouton.addEventListener('click', function () {
                var lignes = conteneur.querySelectorAll('.ligne-parcours');
                if (lignes.length > 1) {
                    ligne.remove();
                    renumeroter();
                }
            });
        }
    }

    if (boutonAjouter && gabarit && conteneur) {
        conteneur.querySelectorAll('.ligne-parcours').forEach(brancherRetrait);

        boutonAjouter.addEventListener('click', function () {
            var html = gabarit.innerHTML.replace(/__INDEX__/g, prochainIndex());
            var wrapper = document.createElement('div');
            wrapper.innerHTML = html.trim();
            var ligne = wrapper.firstElementChild;
            conteneur.appendChild(ligne);
            brancherRetrait(ligne);
        });
    }
})();
