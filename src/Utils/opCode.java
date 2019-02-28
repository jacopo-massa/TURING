package Utils;

/**
 * enum che rappresenta i codici delle operazioni principali e i loro esiti (positivi e negativi)
 */
public enum opCode
{
    /* operazioni principali */

    LOGIN,
    LOGOUT,
    CREATE,
    EDIT,
    END_EDIT,
    SHOW,
    SHOW_ALL,
    INVITE,

    PENDING_INVITATIONS,    // il client richiede inviti pendenti ricevuti mentre era offline
    FILE_LIST,              // il client richiede la lista di file a cui può accedere
    SECTION_RECEIVE,        // il client richiede di ricevere una sezione di un documento

    /* esiti operazioni */

    OP_OK,      // esito positivo dell'operazione richiesta
    OP_FAIL,    // esito negativo dell'operazione richiesta

    /* errori sull'autenticazione/invito utenti */

    ERR_USER_UNKNOWN,           // utente sconosciuto
    ERR_WRONG_PASSWORD,         // password errata
    ERR_USER_ALREADY_LOGGED,    // utente già loggato
    ERR_USER_ALREADY_INVITED,   // utente già invitato a collaborare ad un file
    ERR_OWNER_INVITED,          // invito spedito all'owner del file

    /* errori sulla gestione dei file */
    ERR_FILE_NOT_EXISTS,      // il file richiesto non esiste
    ERR_FILE_ALREADY_EXISTS,  // il file che si vuole creare esiste già (non sono ammessi duplicati per uno stesso utente)
    ERR_PERMISSION_DENIED,    // non si hanno i permessi per visualizzare / editare un certo documento
    SECTION_EDITING           // sezione in fase di editing
}
