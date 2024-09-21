#  Copyright (c) 2024 Cyoda Limited. All rights reserved.
#  This software is the confidential and proprietary information of Cyoda Limited ("Confidential Information").
#  Unauthorized use, disclosure, distribution, or reproduction is prohibited. Any use or access to this software
#  is subject to the terms of the applicable agreements and prior written consent from Cyoda Limited.
import requests
import json
import time
from datetime import datetime
import tzlocal  # For detecting local timezone
from pathlib import Path
import getpass
import os

APPLICATION_JSON = 'application/json'


class CyodaSession:
    def __init__(
            self,
            api_url,
            username='demo.user',
            password_file_path='/Users/paul/.cyoda/demo.passwd',
            password_env_value='DEMO_USER_PASSWD'
    ):
        self.api_url = api_url
        self.login_endpoint = f"{api_url}/auth/login",
        self.token_endpoint = f"{api_url}/auth/token",
        self.username = username
        self.password = os.getenv(password_env_value, None)
        self.password_file_path = password_file_path
        self.access_token = ""

        # Retrieve or ask for password if not set
        if not self.password:
            self.password = self._get_password()

        self.credentials = {
            'username': self.username,
            'password': self.password
        }

    def _get_password(self):
        password_file = Path(self.password_file_path)
        if password_file.exists():
            with password_file.open('r') as file:
                return file.read().rstrip()
        return getpass.getpass("Enter your password: ")

    def connect(self):
        headers = {
            'X-Requested-With': 'XMLHttpRequest',
            'Content-Type': APPLICATION_JSON
        }
        payload = json.dumps(self.credentials)
        response = requests.post(self.login_endpoint, headers=headers, data=payload)

        if response.status_code == 200:
            return response.json().get('refreshToken')
        else:
            raise requests.HTTPError(f"Login failed: {response.status_code} {response.text}")

    def get_access_token(self, refresh_token):
        headers = {
            'Content-Type': APPLICATION_JSON,
            'Authorization': f'Bearer {refresh_token}'
        }
        response = requests.get(self.token_endpoint, headers=headers)

        if response.status_code == 200:
            token_data = response.json()
            self.access_token = token_data.get('token')
            return self.access_token
        else:
            raise requests.HTTPError(f"Token refresh failed: {response.status_code} {response.text}")

    def model_exists(self, model_name, model_version):
        export_model_url = f"{self.api_url}/treeNode/model/export/SIMPLE_VIEW/{model_name}/{model_version}"
        headers = self._get_headers()

        response = requests.get(export_model_url, headers=headers)
        return response.status_code == 200

    def get_model(self, model_name, model_version):
        export_model_url = f"{self.api_url}/treeNode/model/export/SIMPLE_VIEW/{model_name}/{model_version}"
        headers = self._get_headers()

        response = requests.get(export_model_url, headers=headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise requests.HTTPError(f"Getting the model failed: {response.status_code} {response.text}")

    def get_model_state(self, model_name, model_version):
        export_model_url = f"{self.api_url}/treeNode/model/export/SIMPLE_VIEW/{model_name}/{model_version}"
        headers = self._get_headers()

        response = requests.get(export_model_url, headers=headers)
        if response.status_code == 200:
            return response.json().get('currentState')
        else:
            raise requests.HTTPError(f"Failed to get the model: {response.status_code} {response.text}")

    def unlock_model(self, model_name, model_version):
        unlock_model_url = f"{self.api_url}/treeNode/model/{model_name}/{model_version}/unlock"
        headers = self._get_headers()

        response = requests.put(unlock_model_url, headers=headers)
        if response.status_code == 200:
            print('Model unlocked')
        else:
            raise requests.HTTPError(f"Unlock failed: {response.status_code} {response.text}")

    def lock_model(self, model_name, model_version):
        lock_model_url = f"{self.api_url}/treeNode/model/{model_name}/{model_version}/lock"
        headers = self._get_headers()

        response = requests.put(lock_model_url, headers=headers)
        if response.status_code == 200:
            print('Model locked')
        else:
            raise requests.HTTPError(f"Lock failed: {response.status_code} {response.text}")

    def delete_model(self, model_name, model_version):
        model_url = f"{self.api_url}/treeNode/model/{model_name}/{model_version}"
        headers = self._get_headers()

        response = requests.delete(model_url, headers=headers)
        if response.status_code == 200:
            print('Model deleted')
        else:
            raise requests.HTTPError(f"Deletion of the model failed: {response.status_code} {response.text}")

    def delete_all_entities(self, model_name, model_version):
        delete_entities_url = f"{self.api_url}/entity/TREE/{model_name}/{model_version}"
        headers = self._get_headers()

        params = {
            'pageSize': '1000',
            'transactionSize': '1000'
        }
        response = requests.delete(delete_entities_url, headers=headers, params=params)

        if response.status_code == 200:
            return self.calculate_total_entities_removed(response.json())
        else:
            raise requests.HTTPError(f"Deletion failed: {response.status_code} {response.text}")

    def calculate_total_entities_removed(self, data):
        total_entities_removed = 0
        for entry in data:
            total_entities_removed += entry['deleteResult']['numberOfEntititesRemoved']
        return total_entities_removed

    def reset_model(self, model_name, model_version, file_path):
        print(f"Resetting model '{model_name}' version {model_version}")

        if self.model_exists(model_name, model_version):
            print(f"Deleting all data for model '{model_name}' version {model_version}")
            total_entities_deleted = self.delete_all_entities(model_name, model_version)
            print(f"Total entities deleted: {total_entities_deleted}")

            if self.get_model_state(model_name, model_version) == 'LOCKED':
                self.unlock_model(model_name, model_version)
            self.delete_model(model_name, model_version)
        else:
            print(f"Model {model_name} {model_version} doesn't exist. Nothing to delete.")

        with open(file_path, 'r') as file:
            file_contents = json.load(file)

        payload = json.dumps(file_contents)
        model_id = self.derive_model_from_sample_data(model_name, model_version, payload)
        print(f"Model id = {model_id}")
        self.lock_model(model_name, model_version)
        print('Model locked')

    def derive_model_from_sample_data(self, model_name, model_version, payload):
        import_model_url = f"{self.api_url}/treeNode/model/import/JSON/SAMPLE_DATA/{model_name}/{model_version}"
        headers = self._get_headers()

        response = requests.post(import_model_url, headers=headers, data=payload)
        if response.status_code == 200:
            return response.text
        else:
            raise requests.HTTPError(f"Save failed: {response.status_code} {response.text}")

    def create_entity(self, model_name, model_version, json_payload):
        create_entity_url = f"{self.api_url}/entity/JSON/TREE/{model_name}/{model_version}"
        headers = self._get_headers()

        params = {
            'transactionTimeoutMillis': '10000'
        }

        response = requests.post(create_entity_url, headers=headers, params=params, data=json_payload)
        if response.status_code == 200:
            return response.json()[0]['entityIds'][0]
        else:
            raise requests.HTTPError(f"Save failed: {response.status_code} {response.text}")

    def _get_headers(self):
        return {
            'Content-Type': APPLICATION_JSON,
            'Authorization': f'Bearer {self.access_token}'
        }

    def get_all_entities(self, model_name, model_version, page_size, page_number):
        url = f"{self.api_url}/entity/TREE/{model_name}/{model_version}"

        headers = self._get_headers()

        params = {
            'pageSize': f"{page_size}",
            'pageNumber': f"{page_number}"
        }

        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
            return response.json()
        else:
            raise requests.HTTPError(f"Get all entities failed: {response.status_code} {response.text}")

    def create_snapshot_search(self, model_name, model_version, condition):
        url = f"{self.api_url}/treeNode/search/snapshot/{model_name}/{model_version}"
        headers = self._get_headers()

        response = requests.post(url, headers=headers, data=json.dumps(condition))
        if response.status_code == 200:
            return response.json()
        else:
            raise requests.HTTPError(f"Snapshot search trigger failed: {response.status_code} {response.text}")

    def get_snapshot_status(self, snapshot_id):
        url = f"{self.api_url}/treeNode/search/snapshot/{snapshot_id}/status"
        headers = self._get_headers()

        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise requests.HTTPError(f"Snapshot search status check failed: {response.status_code} {response.text}")

    def wait_for_search_completion(self, snapshot_id, timeout=5, interval=10):
        start_time = time.time()

        while True:
            status_response = self.get_snapshot_status(snapshot_id)
            status = status_response.get("snapshotStatus")

            if status == "SUCCESSFUL":
                return status_response
            elif status != "RUNNING":
                raise requests.HTTPError(f"Snapshot search failed: {json.dumps(status_response, indent=4)}")

            elapsed_time = time.time() - start_time
            if elapsed_time > timeout:
                raise TimeoutError(f"Timeout exceeded after {timeout} seconds")

            time.sleep(interval / 1000)

    def get_search_result(self, snapshot_id, page_size, page_number):
        url = f"{self.api_url}/treeNode/search/snapshot/{snapshot_id}"
        headers = self._get_headers()

        params = {
            'pageSize': f"{page_size}",
            'pageNumber': f"{page_number}"
        }

        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
            return response.json()
        else:
            raise requests.HTTPError(f"Get search result failed: {response.status_code} {response.text}")

    def search_entities(self, model_name, model_version, condition):
        snapshot_id = self.create_snapshot_search(model_name, model_version, condition)
        status_response = self.wait_for_search_completion(snapshot_id)
        status_response['snapshotId'] = snapshot_id
        return status_response

    def convert_to_local_time(self, iso_string):
        parsed_date = datetime.fromisoformat(iso_string)
        local_timezone = tzlocal.get_localzone()
        local_date = parsed_date.astimezone(local_timezone)
        return local_date.strftime('%Y-%m-%d %H:%M:%S %Z (%z)')
