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

    function categorieAttendue(ligne) {
        var selectParcours = ligne.querySelector('.select-parcours');
        if (!selectParcours || !selectParcours.value) {
            return null;
        }
        var option = selectParcours.options[selectParcours.selectedIndex];
        var type = option ? option.getAttribute('data-type') : null;
        return type === 'Etude' ? 'DIPLOME' : 'DUREE';
    }

    function filtrerNiveaux(ligne) {
        var selectNiveau = ligne.querySelector('.select-niveau');
        if (!selectNiveau) {
            return;
        }

        var categorie = categorieAttendue(ligne);

        Array.prototype.forEach.call(selectNiveau.options, function (option) {
            if (!option.value) {
                option.hidden = false;
                option.disabled = false;
                return;
            }
            var autorisee = categorie === null || option.getAttribute('data-categorie') === categorie;
            option.hidden = !autorisee;
            option.disabled = !autorisee;
        });

        if (selectNiveau.value) {
            var optionChoisie = selectNiveau.options[selectNiveau.selectedIndex];
            if (optionChoisie && optionChoisie.disabled) {
                selectNiveau.value = '';
            }
        }
    }

    function filtrerQuestions() {
        var selectOffre = document.getElementById('offreId');
        var questions = document.querySelectorAll('.question-item');
        if (!selectOffre || !questions.length) {
            return;
        }

        var offre = selectOffre.value;
        questions.forEach(function (question) {
            var visible = offre !== '' && question.getAttribute('data-offre') === offre;
            question.hidden = !visible;
            question.querySelectorAll('textarea').forEach(function (champ) {
                champ.disabled = !visible;
                if (!visible) {
                    champ.value = '';
                }
            });
        });
    }

    function brancherLigne(ligne) {
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

        var selectParcours = ligne.querySelector('.select-parcours');
        if (selectParcours) {
            selectParcours.addEventListener('change', function () {
                filtrerNiveaux(ligne);
            });
        }

        filtrerNiveaux(ligne);
    }

    var selectOffre = document.getElementById('offreId');
    if (selectOffre) {
        selectOffre.addEventListener('change', filtrerQuestions);
        filtrerQuestions();
    }

    if (boutonAjouter && gabarit && conteneur) {
        conteneur.querySelectorAll('.ligne-parcours').forEach(brancherLigne);

        boutonAjouter.addEventListener('click', function () {
            var html = gabarit.innerHTML.replace(/__INDEX__/g, prochainIndex());
            var wrapper = document.createElement('div');
            wrapper.innerHTML = html.trim();
            var ligne = wrapper.firstElementChild;
            conteneur.appendChild(ligne);
            brancherLigne(ligne);
        });
    }
})();
