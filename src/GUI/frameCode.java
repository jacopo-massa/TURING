package GUI;

/**
 * enum che rappresenta il tipo di pannello che voglio visualizzare
 */
public enum frameCode
{
    LOGIN,  // pannello per effettuare login/registrazione
    TURING, // pannello principale con i bottoni per le varie operazioni, e l'area per la chat
    CREATE, // pannello per creare un nuovo file da mandare al server
    SHOW,   // pannello per richiedere la visualizzazione di un documento o di una sua sezione
    INVITE, // pannello per invitare utenti a collaborare all'editing di un file
    EDIT,   // pannello per richiedere di poter editare la sezione di un file
    SERVER  // pannello di log degli eventi nel server
}
