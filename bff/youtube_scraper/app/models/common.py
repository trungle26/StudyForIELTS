from datetime import datetime
from typing import Any

from bson import ObjectId


def serialize_mongo_value(value: Any) -> Any:
    if isinstance(value, ObjectId):
        return str(value)
    if isinstance(value, datetime):
        return value
    if isinstance(value, list):
        return [serialize_mongo_value(item) for item in value]
    if isinstance(value, dict):
        return {key: serialize_mongo_value(item) for key, item in value.items()}
    return value

