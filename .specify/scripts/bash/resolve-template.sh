#!/usr/bin/env bash

#
# Copyright (c) 2026.  Roland T. Lichti <rlichti@kaiserpfalz-edv.de>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# ERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
# You may contact me via email rlichti@kaiserpfalz-edv.de or via mail
#
# Kaiserpfalz EDV-Service
# Roland T. Lichti
# Darmstädter Str. 12
# 64625 Bensheim
# GERMANY
#

set -e

SCRIPT_DIR="$(CDPATH="" cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

JSON_MODE=false
TEMPLATE_NAME=""

for arg in "$@"; do
    case "$arg" in
        --json) JSON_MODE=true ;;
        --help|-h)
            echo "Usage: $0 <template-name> [--json]"
            exit 0
            ;;
        -*)
            echo "ERROR: Unknown option '$arg'" >&2
            exit 1
            ;;
        *)
            if [[ -n "$TEMPLATE_NAME" ]]; then
                echo "ERROR: Unexpected argument '$arg'" >&2
                exit 1
            fi
            TEMPLATE_NAME="$arg"
            ;;
    esac
done

if [[ -z "$TEMPLATE_NAME" ]]; then
    echo "ERROR: Template name is required" >&2
    exit 1
fi

REPO_ROOT=$(get_repo_root)
if TEMPLATE_CONTENT=$(resolve_template_content "$TEMPLATE_NAME" "$REPO_ROOT"; status=$?; printf x; exit "$status"); then
    TEMPLATE_CONTENT="${TEMPLATE_CONTENT%x}"
else
    echo "ERROR: Could not resolve required $TEMPLATE_NAME from the template override stack for $REPO_ROOT" >&2
    exit 1
fi

if $JSON_MODE; then
    if has_jq; then
        jq -cn \
            --arg template_name "$TEMPLATE_NAME" \
            --arg template_content "$TEMPLATE_CONTENT" \
            '{TEMPLATE_NAME:$template_name,TEMPLATE_CONTENT:$template_content}'
    else
        printf '{"TEMPLATE_NAME":"%s","TEMPLATE_CONTENT":"%s"}\n' \
            "$(json_escape "$TEMPLATE_NAME")" "$(json_escape "$TEMPLATE_CONTENT")"
    fi
else
    printf '%s' "$TEMPLATE_CONTENT"
fi
