package dev.bxane.rynx.playback.catalog

/** A provider explicitly denied this item; trying another extractor must not bypass that decision. */
class ProviderAccessRefusalException(cause: Throwable? = null) :
    IllegalStateException("The media provider refused playback for this item.", cause)
