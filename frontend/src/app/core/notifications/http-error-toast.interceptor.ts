import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from './notification.service';

export const httpErrorToastInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.includes('/api/')) {
    return next(request);
  }

  const notifications = inject(NotificationService);

  return next(request).pipe(catchError((error: unknown) => {
    if (error instanceof HttpErrorResponse) {
      const isSessionRestore = request.url.endsWith('/api/auth/me');
      if (isSessionRestore && error.status === 401) {
        return throwError(() => error);
      }

      const isTimeManagementRequest = /\/api\/(planning|manager\/planning|manager\/presence|rh\/planning|rh\/presence|administration\/planning|administration\/presence)(\/|$)/.test(request.url);
      if (!isTimeManagementRequest) {
        const body = error.error as { detail?: string; message?: string } | null;
        const backendMessage = body?.detail ?? body?.message;
        const { summary, detail } = getErrorMessage(request.url, request.method, error, backendMessage);
        notifications.error(summary, detail);
      }
    }

    return throwError(() => error);
  }));
};

function getErrorMessage(
  url: string,
  method: string,
  error: HttpErrorResponse,
  backendMessage?: string
): { summary: string; detail: string } {
  if (url.endsWith('/api/auth/login')) {
    if (error.status === 0) {
      return { summary: 'Connexion impossible', detail: 'Le serveur SIRH ne répond pas. Vérifiez que Spring Boot est démarré.' };
    }
    if (error.status === 401) {
      return { summary: 'Connexion refusée', detail: backendMessage ?? 'Identifiant ou mot de passe incorrect, ou compte suspendu.' };
    }
    if (error.status === 403) {
      return { summary: 'Connexion à renouveler', detail: 'La vérification de sécurité a expiré. Réessayez.' };
    }
    if (error.status === 400) {
      return { summary: 'Informations manquantes', detail: 'Saisissez votre identifiant et votre mot de passe.' };
    }
    return serverOrGeneric(error.status, 'Connexion impossible', backendMessage);
  }

  if (url.endsWith('/api/auth/register')) {
    if (error.status === 0) {
      return { summary: 'Inscription non envoyée', detail: 'Le serveur ne répond pas. Aucune demande de création n’a été enregistrée.' };
    }
    if (error.status === 409) {
      return { summary: 'Identifiant ou e-mail déjà utilisé', detail: backendMessage ?? 'Choisissez un autre nom d’utilisateur ou une autre adresse e-mail.' };
    }
    if (error.status === 400) {
      return { summary: 'Informations d’inscription invalides', detail: 'Vérifiez le nom complet, l’identifiant (3 à 50 caractères), l’e-mail et le mot de passe (8 à 72 caractères).' };
    }
    if (error.status === 401) {
      return { summary: 'Inscription refusée', detail: 'Le serveur demande une authentification. Redémarrez Spring Boot pour charger la configuration actuelle.' };
    }
    if (error.status === 403) {
      return { summary: 'Vérification de sécurité refusée', detail: 'Rechargez la page puis renvoyez le formulaire d’inscription.' };
    }
    if (error.status === 404) {
      return { summary: 'Service d’inscription introuvable', detail: 'Redémarrez Spring Boot pour charger la route /api/auth/register.' };
    }
    if (error.status === 413) {
      return { summary: 'Formulaire trop volumineux', detail: 'Réduisez la taille des informations saisies puis réessayez.' };
    }
    if (error.status === 429) {
      return { summary: 'Trop de tentatives', detail: 'Attendez un instant avant de renvoyer le formulaire.' };
    }
    if (error.status === 405) {
      return { summary: 'Service d’inscription mal configuré', detail: 'La route existe mais n’accepte pas cette méthode. Redémarrez Spring Boot.' };
    }
    if (error.status === 415) {
      return { summary: 'Format du formulaire refusé', detail: 'Le serveur attend des données JSON. Rechargez la page puis réessayez.' };
    }
    if (error.status === 422) {
      return { summary: 'Données d’inscription refusées', detail: backendMessage ?? 'Vérifiez le format de chaque champ et réessayez.' };
    }
    if (error.status >= 500) {
      return { summary: 'Création du compte échouée', detail: 'Le serveur n’a pas pu enregistrer le compte. Réessaie dans quelques instants.' };
    }
    return serverOrGeneric(error.status, 'Création du compte impossible', backendMessage);
  }

  if (error.status === 0) {
    return { summary: 'Serveur indisponible', detail: 'Connexion au serveur impossible. Vérifiez que Spring Boot est démarré.' };
  }

  if (url.endsWith('/api/auth/csrf')) {
    return serverOrGeneric(error.status, 'Vérification de sécurité impossible', 'Rechargez la page puis réessayez.');
  }

  if (url.endsWith('/api/auth/logout')) {
    return serverOrGeneric(error.status, 'Déconnexion incomplète', 'La session locale sera fermée. Réessayez si nécessaire.');
  }

  if (url.includes('/api/manager/recrutement/options')) {
    if (error.status === 401) {
      return { summary: 'Session manager expirée', detail: 'Reconnectez-vous pour charger le formulaire de recrutement.' };
    }
    if (error.status === 403) {
      return { summary: 'Accès manager requis', detail: 'Cette page est réservée aux comptes manager.' };
    }
    if (error.status === 409) {
      return { summary: 'Département manager manquant', detail: backendMessage ?? 'Le compte doit être rattaché à un seul département actif.' };
    }
    return serverOrGeneric(error.status, 'Chargement du formulaire impossible', 'Le département ou les postes ne sont pas disponibles. Réessayez.');
  }

  if (url.endsWith('/api/manager/recrutement/demandes') && method === 'POST') {
    if (error.status === 401) {
      return { summary: 'Session manager expirée', detail: 'Reconnectez-vous avant d’envoyer la demande.' };
    }
    if (error.status === 400) {
      return { summary: 'Demande à corriger', detail: backendMessage ?? 'Vérifiez le poste, le motif, l’effectif et le budget par personne.' };
    }
    if (error.status === 409) {
      return { summary: 'Demande non envoyée', detail: backendMessage ?? 'Le département ou la fourchette salariale du poste doit être vérifié.' };
    }
    if (error.status === 403) {
      return { summary: 'Accès manager requis', detail: 'Seul un manager peut envoyer une demande de recrutement.' };
    }
    return serverOrGeneric(error.status, 'Envoi de la demande impossible', 'La demande n’a pas été enregistrée. Réessayez.');
  }

  if (url.endsWith('/api/manager/recrutement/demandes') && method === 'GET') {
    if (error.status === 401) {
      return { summary: 'Session manager expirée', detail: 'Reconnectez-vous pour consulter vos demandes.' };
    }
    if (error.status === 403) {
      return { summary: 'Accès manager requis', detail: 'Le suivi des demandes est réservé aux comptes manager.' };
    }
    return serverOrGeneric(error.status, 'Demandes indisponibles', 'Impossible de charger le suivi de vos demandes. Réessayez.');
  }

  if (/\/api\/manager\/recrutement\/demandes\/\d+\/annuler$/.test(url) && method === 'POST') {
    if (error.status === 401) {
      return { summary: 'Session manager expirée', detail: 'Reconnectez-vous avant d’annuler cette demande.' };
    }
    if (error.status === 403) {
      return { summary: 'Accès au département refusé', detail: 'Seul un manager rattaché au département de cette demande peut l’annuler.' };
    }
    if (error.status === 404) {
      return { summary: 'Demande introuvable', detail: 'Elle a peut-être déjà été supprimée ou ne vous appartient pas.' };
    }
    if (error.status === 409) {
      return { summary: 'Annulation impossible', detail: backendMessage ?? 'Le traitement de cette demande a déjà commencé.' };
    }
    return serverOrGeneric(error.status, 'Annulation impossible', 'La demande est toujours enregistrée. Réessayez.');
  }

  if (/\/api\/manager\/recrutement\/demandes\/\d+$/.test(url) && method === 'GET') {
    if (error.status === 401) {
      return { summary: 'Session manager expirée', detail: 'Reconnectez-vous pour consulter cette demande.' };
    }
    if (error.status === 404) {
      return { summary: 'Demande introuvable', detail: 'Cette demande n’existe pas ou ne relève pas de votre département.' };
    }
    return serverOrGeneric(error.status, 'Détail indisponible', 'Impossible de charger le détail de cette demande. Réessayez.');
  }

  if (/\/api\/(rh|finance)\/recrutement\/demandes/.test(url)) {
    const area = url.includes('/api/finance/') ? 'Finance' : 'RH';
    if (error.status === 401) return { summary: 'Session expirée', detail: `Reconnectez-vous pour accéder à la file de validation ${area}.` };
    if (error.status === 403) return { summary: 'Accès refusé', detail: area === 'Finance' ? 'Votre compte doit être affecté au département Finance.' : 'Cette file est réservée aux comptes RH.' };
    if (error.status === 409) return { summary: 'Demande déjà traitée', detail: backendMessage ?? 'La demande a changé d’étape et n’est plus dans cette file.' };
    if (error.status === 400) return { summary: 'Décision incomplète', detail: backendMessage ?? 'Vérifiez votre décision et le commentaire de refus.' };
    if (error.status === 404) return { summary: 'Demande introuvable', detail: 'Elle a été supprimée ou se trouve maintenant dans une autre étape.' };
    return serverOrGeneric(error.status, 'Validation impossible', 'Impossible de charger ou traiter cette demande. Réessayez.');
  }

  if (url.startsWith('/api/rh/candidatures')) {
    if (error.status === 401) return { summary: 'Session RH expirée', detail: 'Reconnectez-vous pour consulter les candidatures reçues.' };
    if (error.status === 403) return { summary: 'Accès RH requis', detail: 'Les candidatures sont réservées aux comptes RH.' };
    if (error.status === 404) return { summary: 'Fichier introuvable', detail: 'Le CV ou la photo n’est plus disponible.' };
    return serverOrGeneric(error.status, 'Candidatures indisponibles', 'Impossible de charger les dossiers reçus. Réessayez.');
  }

  if (url.startsWith('/api/candidatures')) {
    if (error.status === 401) return { summary: 'Connexion requise', detail: 'Connectez-vous avec votre compte candidat pour postuler.' };
    if (error.status === 403) return { summary: 'Compte non validé', detail: backendMessage ?? 'Seuls les comptes validés peuvent envoyer une candidature.' };
    if (error.status === 404) return { summary: 'Offre indisponible', detail: backendMessage ?? 'Cette offre est clôturée ou n’existe plus.' };
    if (error.status === 409) return { summary: 'Candidature déjà envoyée', detail: backendMessage ?? 'Vous avez déjà postulé à cette offre.' };
    if (error.status === 400) return { summary: 'Dossier à corriger', detail: backendMessage ?? 'Vérifiez les fichiers et les informations de votre parcours.' };
    return serverOrGeneric(error.status, 'Candidature impossible', 'Votre dossier n’a pas été envoyé. Réessayez.');
  }

  if (url.includes('/api/administration/recrutement/demandes')) {
    if (error.status === 401) return { summary: 'Session expirée', detail: 'Reconnectez-vous pour accéder aux demandes approuvées.' };
    if (error.status === 403) return { summary: 'Accès Administration requis', detail: 'Seul le rôle Administration peut créer une offre à partir d’une demande.' };
    if (error.status === 400) return { summary: 'Informations à corriger', detail: backendMessage ?? 'Vérifiez le titre, le texte et la date limite de l’offre.' };
    if (error.status === 409) return { summary: 'Demande déjà traitée', detail: backendMessage ?? 'Une offre a peut-être déjà été créée pour cette demande.' };
    return serverOrGeneric(error.status, 'Création de l’offre impossible', 'La demande reste enregistrée. Réessayez.');
  }

  if (url.startsWith('/api/offres')) {
    if (error.status === 404) return { summary: 'Offre indisponible', detail: backendMessage ?? 'Cette offre a été clôturée ou retirée.' };
    return serverOrGeneric(error.status, 'Offres indisponibles', 'Impossible de charger les offres. Réessayez dans un instant.');
  }

  if (url.startsWith('/api/notifications')) {
    if (error.status === 401) return { summary: 'Session expirée', detail: 'Reconnectez-vous pour consulter vos notifications.' };
    if (error.status === 404) return { summary: 'Notification introuvable', detail: 'Elle a déjà été supprimée ou ne vous appartient pas.' };
    return serverOrGeneric(error.status, 'Notifications indisponibles', 'Impossible de charger ou modifier cette notification. Réessayez.');
  }

  if (url.startsWith('/api/gestionnaire/')) {
    if (error.status === 401) return { summary: 'Session expirée', detail: 'Reconnectez-vous pour gérer les comptes utilisateurs.' };
    if (error.status === 403) return { summary: 'Accès gestionnaire requis', detail: 'Cette gestion est réservée au rôle Gestionnaire des comptes.' };
    if (error.status === 400) return { summary: 'Informations à corriger', detail: backendMessage ?? 'Vérifiez les informations du compte et son rattachement.' };
    if (error.status === 404) return { summary: 'Compte introuvable', detail: backendMessage ?? 'Le compte a été supprimé ou n’existe plus.' };
    if (error.status === 409) return { summary: 'Action impossible', detail: backendMessage ?? 'Cette modification entre en conflit avec l’état du compte.' };
    return serverOrGeneric(error.status, 'Gestion des comptes indisponible', 'Impossible de charger ou modifier les comptes. Réessayez.');
  }

  if (error.status === 401) {
    return { summary: 'Session expirée', detail: 'Reconnectez-vous pour continuer.' };
  }
  if (error.status === 403) {
    return { summary: 'Accès refusé', detail: 'Votre compte ne possède pas les droits nécessaires pour cette action.' };
  }
  if (error.status === 404) {
    return { summary: 'Page ou donnée introuvable', detail: backendMessage ?? 'La ressource demandée n’existe plus.' };
  }
  if (error.status === 409) {
    return { summary: 'Conflit de données', detail: backendMessage ?? 'Cette action entre en conflit avec une donnée existante.' };
  }
  if (error.status === 400) {
    return { summary: 'Données à vérifier', detail: backendMessage ?? 'Certaines informations saisies sont invalides.' };
  }
  return serverOrGeneric(error.status, 'Action impossible', backendMessage);
}

function serverOrGeneric(status: number, summary: string, detail?: string): { summary: string; detail: string } {
  if (status >= 500) {
    return { summary: 'Erreur du serveur', detail: detail ?? 'Une erreur est survenue. Réessayez dans quelques instants.' };
  }
  return { summary, detail: detail ?? 'Vérifiez les informations puis réessayez.' };
}
