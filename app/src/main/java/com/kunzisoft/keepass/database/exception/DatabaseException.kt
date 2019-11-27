package com.kunzisoft.keepass.database.exception

import android.content.res.Resources
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.PwNodeId
import com.kunzisoft.keepass.database.element.Type

abstract class DatabaseException : Exception {

    abstract var errorId: Int
    var parameters: (Array<out String>)? = null

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
    constructor(throwable: Throwable) : super(throwable)

    fun getLocalizedMessage(resources: Resources): String {
        parameters?.let {
            return resources.getString(errorId, *it)
        } ?: return resources.getString(errorId)
    }
}

open class LoadDatabaseException : DatabaseException {

    @StringRes
    override var errorId: Int = R.string.error_load_database
    constructor() : super()
    constructor(throwable: Throwable) : super(throwable)
}

class ArcFourDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_arc4
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class FileNotFoundDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.file_not_found_content
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class InvalidAlgorithmDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_algorithm

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class DuplicateUuidDatabaseException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_db_same_uuid
    constructor(type: Type, uuid: PwNodeId<*>) : super() {
        parameters = arrayOf(type.name, uuid.toString())
    }
    constructor(exception: Throwable) : super(exception)
}

class IODatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_load_database
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class KDFMemoryDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_load_database_KDF_memory
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class SignatureDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_db_sig
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class VersionDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.unsupported_db_version
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class InvalidCredentialsDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_credentials
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class KeyFileEmptyDatabaseException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.keyfile_is_empty
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class NoMemoryDatabaseException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_out_of_memory
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class EntryDatabaseException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_move_entry_here
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class MoveGroupDatabaseException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_move_folder_in_itself
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class CopyEntryDatabaseException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_copy_entry_here
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class CopyGroupDatabaseException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_copy_group_here
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

// TODO Output Exception
open class DatabaseOutputException : DatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_save_database
    constructor(string: String) : super(string)
    constructor(string: String, e: Exception) : super(string, e)
    constructor(e: Exception) : super(e)
}