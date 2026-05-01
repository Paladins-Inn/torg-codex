#  Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU Affero General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU Affero General Public License for more details.
#
#  You should have received a copy of the GNU Affero General Public License
#  along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
#  You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
#
#  Kaiserpfalz EDV-Service
#  Roland T. Lichti
#  Darmstädter Str. 12
#  64625 Bensheim
#  GERMANY

import os
import uuid
import yaml
import csv

def convert_data():
    db_dir = 'torg-codex-data/src/main/resources/db'
    load_dir = os.path.join(db_dir, 'load')
    id_map = {}

    # First pass: generate UUIDs and store them in id_map
    for root, _, files in os.walk(db_dir):
        for file in files:
            if file.endswith('.yml') and file not in ['torg-data-entity.yml', 'torg-data-load.yml']:
                filepath = os.path.join(root, file)
                entity_name = file.split('.')[0]
                if entity_name not in id_map:
                    id_map[entity_name] = {}

                with open(filepath, 'r') as f:
                    data = yaml.safe_load(f)
                    if isinstance(data, list):
                        for item in data:
                            if 'name' in item:
                                old_id = item.get('id', item['name'])
                                new_id = str(uuid.uuid4())
                                id_map[entity_name][old_id] = new_id
                                item['id'] = new_id


    # Second pass: process and write to CSV
    for root, _, files in os.walk(db_dir):
        for file in files:
            if file.endswith('.yml') and file not in ['torg-data-entity.yml', 'torg-data-load.yml']:
                filepath = os.path.join(root, file)
                entity_name = file.split('.')[0]
                table_name = f"torg_{entity_name.replace('s', '')}"
                if entity_name.endswith('es'):
                     table_name = f"torg_{entity_name[:-2]}"
                elif entity_name.endswith('s'):
                    table_name = f"torg_{entity_name[:-1]}"


                csv_filename = os.path.join(load_dir, f'{table_name}.csv')

                with open(filepath, 'r') as f:
                    data = yaml.safe_load(f)

                if isinstance(data, list) and data:
                    # Remap foreign keys
                    for item in data:
                        for key, value in item.items():
                            if isinstance(value, str):
                                for entity, mapping in id_map.items():
                                    if value in mapping:
                                        item[key] = mapping[value]
                                        break
                            elif isinstance(value, list):
                                new_list = []
                                for sub_item in value:
                                    if isinstance(sub_item, str):
                                        found = False
                                        for entity, mapping in id_map.items():
                                            if sub_item in mapping:
                                                new_list.append(mapping[sub_item])
                                                found = True
                                                break
                                        if not found:
                                            new_list.append(sub_item)
                                    else:
                                        new_list.append(sub_item)
                                item[key] = new_list


                    # Write to CSV
                    with open(csv_filename, 'w', newline='') as csvfile:
                        # Get headers from the first item, ensuring id is first
                        headers = ['id'] + [k for k in data[0].keys() if k != 'id']
                        writer = csv.DictWriter(csvfile, fieldnames=headers, delimiter=';')
                        writer.writeheader()
                        for item in data:
                            # Ensure all keys are present, fill with empty string if not
                            row = {h: item.get(h, '') for h in headers}
                            writer.writerow(row)
                    print(f"Converted {file} to {csv_filename}")

if __name__ == '__main__':
    convert_data()

