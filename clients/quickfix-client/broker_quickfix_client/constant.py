from quickfix import DataDictionary

from broker_quickfix_client.utils.loader import get_config_dir

DATA_DICTIONNARY: DataDictionary = DataDictionary(str(get_config_dir() / "FIX44.xml"))

SERVER_IP = "127.0.0.1"

SERVER_PORT = 5001

SERVER_NAME = "SERVER"
