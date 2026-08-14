package com.orgzly.android.repos

enum class RepoType(val id: Int) {
    MOCK(0),
    DIRECTORY(1),
    DROPBOX(2),
    WEBDAV(3),
    GIT(4),
    DOCUMENT(5),
    DATABASE(6);

    companion object {
        @JvmStatic
        fun fromId(id: Int): RepoType {
            return entries.first { it.id == id }
        }
    }
}
