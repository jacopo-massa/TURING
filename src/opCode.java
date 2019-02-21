public enum opCode
{
    LOGIN,
    LOGOUT,
    CREATE,
    EDIT,
    END_EDIT,
    SHOW,
    SHOW_ALL,
    FILE_LIST,

    OP_OK,
    OP_FAIL,


    ERR_USER_UNKNOWN,
    ERR_WRONG_PASSWORD,
    ERR_USER_ALREADY_LOGGED,
    ERR_FILE_LIST,
    ERR_FILE_NOT_EXISTS,
    ERR_FILE_ALREADY_EXISTS;
}
