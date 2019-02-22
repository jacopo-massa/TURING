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

    FILE_LIST,
    SECTION_SEND,
    SECTION_RECEIVE,

    /* esiti operazioni */
    OP_OK,
    OP_FAIL,

    /* errori sull'autenticazione utenti */
    ERR_USER_UNKNOWN,
    ERR_WRONG_PASSWORD,
    ERR_USER_ALREADY_LOGGED,

    /* errori sulla gestione dei file */
    ERR_FILE_LIST,
    ERR_FILE_NOT_EXISTS,
    ERR_FILE_ALREADY_EXISTS,
    ERR_PERMISSION_DENIED,
    SECTION_EDITING;
}
