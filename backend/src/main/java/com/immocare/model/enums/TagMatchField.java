package com.immocare.model.enums;

/**
 * Fields used for matching tag learning rules against transaction data.
 * BR-UC014-18: valid values are COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE.
 * COUNTERPARTY_NAME is intentionally excluded.
 */
public enum TagMatchField {
    COUNTERPARTY_ACCOUNT,
    DESCRIPTION,
    ASSET_TYPE
}
