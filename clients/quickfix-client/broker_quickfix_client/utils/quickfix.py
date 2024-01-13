import logging

from quickfix import (
    DataDictionary,
    Dictionary,
    FieldBase,
    Message,
    MsgType,
    MsgType_Heartbeat,
    SessionID,
    SessionSettings,
)

from broker_quickfix_client.constant import (
    DATA_DICTIONNARY,
    SERVER_IP,
    SERVER_NAME,
    SERVER_PORT,
)
from broker_quickfix_client.decorators import default_return_value_decorator

logger = logging.getLogger("quickfix.event")


def log_quick_fix_message(
    message: Message,
    prefix: str | None,
    level: int = logging.DEBUG,
    data_dictionary: DataDictionary = DATA_DICTIONNARY,
):
    def get_field_name(key):
        field_name = key
        ret = data_dictionary.getFieldName(field=int(key), name=field_name)
        return ret[0]

    def get_field_value(key, value):
        field_value = value
        ret = data_dictionary.getValueName(
            field=int(key), value=value, name=field_value
        )
        return ret[0]

    if is_heartbeat(message):
        return

    message_parts = [
        "=".join([get_field_name(split[0]), get_field_value(split[0], split[1])])
        if len(split := s.split("=")) == 2
        else s
        for s in str(message).split("\x01")
    ]

    message_string = "|".join(message_parts)
    logger.log(level, f"{prefix}: {message_string}")


def is_heartbeat(message: Message) -> bool:
    return message.getHeader().getField(MsgType()).getString() == MsgType_Heartbeat


@default_return_value_decorator(None)
def get_message_field(message: Message, field_type: type[FieldBase]) -> str:
    field = field_type()
    message.getField(field)
    return field.getString()


def set_settings(username: str):
    settings = SessionSettings()

    # Default settings
    default_dict = Dictionary()

    default_dict.setString("FileStorePath", "./storage/")
    default_dict.setString("FileLogPath", "./logs/client")
    default_dict.setBool("ResetOnLogon", True)
    default_dict.setBool("ResetOnLogout", True)
    default_dict.setBool("ResetOnDisconnect", True)
    default_dict.setBool("UseDataDictionary", True)

    settings.set(default_dict)

    # User settings
    session_id = SessionID("FIX.4.4", username, SERVER_NAME)
    session_dict = Dictionary()

    session_dict.setString("ConnectionType", "initiator")
    session_dict.setString("SocketConnectHost", SERVER_IP)
    session_dict.setInt("SocketConnectPort", SERVER_PORT)
    session_dict.setString("DataDictionary", "./config/FIX44.xml")
    session_dict.setString("StartTime", "00:00:00")
    session_dict.setString("EndTime", "00:00:00")
    session_dict.setInt("HeartBtInt", 30)

    settings.set(session_id, session_dict)

    return settings
