

import argparse
import os
import sys
from enum import Enum
from getpass import getpass

import requests
import json

RESPONSE_ = "response="


# Script for export/import cyoda configs
# useful when you need to install updated schema with clear tables in cassandra

# How to run it
# if you prefer store password in file
# !python cyoda_config_ctl.py --mode <export/import> --host "https://my-env.cyoda.net/api" --username your_username --password "your_pass"  --folder_for_save_export_configs "/home/alex/Downloads/test" --passwordFile /tmp/pass.txt

# if you prefer input password manually. just enter it in input after command
# python3 backup_configs.py --mode <export/import> --host "https://my-env.cyoda.net/api" --username your_username --password "your_pass"  --folder_for_save_export_configs "/home/alex/Downloads/test"

class Mode(Enum):
    EXPORT = 'export'
    IMPORT = 'import'

    def __str__(self):
        return self.value


def login(password_value):
    url = args.host + "/auth/login"

    data = {
        "username": args.username,
        "password": password_value,
    }

    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json; charset=utf-8",
        "X-Requested-With": "XMLHttpRequest",
    }
    response = requests.post(url, data=json.dumps(data), headers=headers)

    if response.status_code != 200:
        print("invalid username or password. response code=" + str(response.status_code))
        print(RESPONSE_ + str(response.text))
        sys.exit(1)

    parsed_response = response.json()

    if "token" in parsed_response:
        return parsed_response["token"]
    else:
        print("can not find token in response=" + parsed_response)
        sys.exit(1)


def abstract_export_data(is_need_export, endpoint, output_json):
    if not is_need_export:
        print("skipping export " + endpoint)
        return

    url = args.host + endpoint

    response = requests.get(url, headers=auth_headers)

    if response.status_code != 200:
        print("can not export " + endpoint)
        print(RESPONSE_ + str(response.text))
        sys.exit(1)

    file_path = args.folder_for_save_export_configs + "/" + output_json
    json_data = response.json()

    file = open(file_path, "w")
    file.write(json.dumps(json_data))
    file.close()
    print("saved " + file_path)


def abstract_import_data(is_need_import, endpoint, file_name):
    if not is_need_import:
        print("skipping import " + endpoint)
        return

    url = args.host + endpoint
    file_path = args.folder_for_save_export_configs + "/" + file_name
    file = open(file_path, "r")
    data = file.read()
    file.close()
    headers = {"Content-Type": "application/json; charset=utf-8"}

    response = requests.post(url, data=data, headers={**auth_headers, **headers})

    if response.status_code != 200:
        print("can not import " + file_name)
        print(RESPONSE_ + str(response.text))
        sys.exit(1)
    else:
        print("imported " + file_path)


def export_distributed_reporting():
    abstract_export_data(
        is_need_export=args.need_to_export_stream_data,
        endpoint="/platform-api/reporting/export-all",
        output_json="distributed_reporting.json"
    )


def export_stream_data():
    abstract_export_data(
        is_need_export=args.need_to_export_stream_data,
        endpoint="/platform-api/stream-data/export-all",
        output_json="stream_data.json"
    )


def export_alias_catalog():
    abstract_export_data(
        is_need_export=args.need_to_export_alias_catalog,
        endpoint="/platform-api/catalog/item/export-all",
        output_json="alias_catalog.json"
    )


def export_composite_indexes():
    abstract_export_data(
        is_need_export=args.need_to_export_composite_indexes,
        endpoint="/platform-common/composite-indexes/export-all",
        output_json="composite_indexes.json"
    )


def export_statemachine():
    abstract_export_data(
        is_need_export=args.need_to_export_state_machine,
        endpoint="/platform-api/statemachine/export?includeIds=",
        output_json="statemachine.json"
    )


def export_cobi():
    abstract_export_data(
        is_need_export=args.need_to_export_cobi,
        endpoint="/data-source-config/export-all-cobi",
        output_json="cobi.json"
    )


def import_distributed_reporting():
    abstract_import_data(
        is_need_import=args.need_to_import_distributed_reporting,
        endpoint="/platform-api/reporting/import",
        file_name="distributed_reporting.json"
    )


def import_stream_data():
    abstract_import_data(
        is_need_import=args.need_to_import_stream_data,
        endpoint="/platform-api/stream-data/import",
        file_name="stream_data.json"
    )


def import_alias_catalog():
    abstract_import_data(
        is_need_import=args.need_to_import_alias_catalog,
        endpoint="/platform-api/catalog/item/import?needRewrite=true",
        file_name="alias_catalog.json"
    )


def import_composite_indexes():
    abstract_import_data(
        is_need_import=args.need_to_import_composite_indexes,
        endpoint="/platform-common/composite-indexes/import",
        file_name="composite_indexes.json"
    )


def import_statemachine():
    abstract_import_data(
        is_need_import=args.need_to_import_state_machine,
        endpoint="/platform-api/statemachine/import?needRewrite=true",
        file_name="statemachine.json"
    )


def import_cobi():
    abstract_import_data(
        is_need_import=args.need_to_import_cobi,
        endpoint="/data-source-config/import-cobi-config?doPostProcess=true",
        file_name="cobi.json"
    )


def get_folder_root():
    folder = args.folder_for_save_export_configs
    if not os.path.exists(folder):
        os.makedirs(folder)
    return folder


def get_auth_headers():
    return {"Authorization": "Bearer " + token}


def check_python_version():
    if sys.version_info <= (3, 0):
        print("need to use python3.x for execute this script")
        sys.exit(1)


def parse_arguments():
    parser = argparse.ArgumentParser(
        description='Export/Import configs from Cyoda',
        exit_on_error=True
    )
    parser.add_argument('-m', '--mode', type=Mode, required=True, choices=list(Mode))

    parser.add_argument('-u', '--username', type=str, required=True, help="username for login to Cyoda")

    parser.add_argument('-pf', '--passwordFile', type=str, required=False, help='file with password for login to Cyoda')
    parser.add_argument('-pw', '--password', type=str, required=False, help='the password to log into Cyoda, if you do not have a password file')

    parser.add_argument('-host', '--host', type=str, required=True, help="host like https://dev.cyoda.com/api")
    parser.add_argument('-fd', '--folder_for_save_export_configs', type=str, required=True)

    parser.add_argument('--need_to_export_distributed_reporting', type=bool, required=False, default=True)
    parser.add_argument('--need_to_export_stream_data', type=bool, required=False, default=True)
    parser.add_argument('--need_to_export_alias_catalog', type=bool, required=False, default=True)
    parser.add_argument('--need_to_export_composite_indexes', type=bool, required=False, default=True)
    parser.add_argument('--need_to_export_state_machine', type=bool, required=False, default=False)
    parser.add_argument('--need_to_export_cobi', type=bool, required=False, default=True)

    parser.add_argument('--need_to_import_distributed_reporting', type=bool, required=False, default=True)
    parser.add_argument('--need_to_import_stream_data', type=bool, required=False, default=True)
    parser.add_argument('--need_to_import_alias_catalog', type=bool, required=False, default=True)
    parser.add_argument('--need_to_import_composite_indexes', type=bool, required=False, default=True)
    parser.add_argument('--need_to_import_state_machine', type=bool, required=False, default=False)
    parser.add_argument('--need_to_import_cobi', type=bool, required=False, default=True)

    return parser.parse_args()


def start_export():
    export_distributed_reporting()
    export_stream_data()
    export_alias_catalog()
    export_composite_indexes()
    export_statemachine()
    export_cobi()

    print("finish export configs")


def start_imports():
    import_distributed_reporting()
    import_stream_data()
    import_alias_catalog()
    import_composite_indexes()
    import_statemachine()
    import_cobi()

    print("finish import configs")


if __name__ == '__main__':
    check_python_version()
    args = parse_arguments()

    password = None
    passwordFile = args.passwordFile
    if passwordFile is None:
        password = args.password
    else:
        password = open(passwordFile, "r").readline().rstrip()

    token = login(password)
    auth_headers = get_auth_headers()
    folder_for_save = get_folder_root()

    if args.mode == Mode.EXPORT:
        start_export()
    else:
        start_imports()
