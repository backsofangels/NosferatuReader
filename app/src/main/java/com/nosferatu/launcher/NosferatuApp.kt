/*
 * Copyright (c) 2026 nosferatu-launcher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nosferatu.launcher

import android.app.Application
import com.nosferatu.launcher.data.EbookFormat
import com.nosferatu.launcher.data.database.AppDatabase
import com.nosferatu.launcher.library.*
import com.nosferatu.launcher.parser.BookParser
import com.nosferatu.launcher.parser.EpubParser
import com.nosferatu.launcher.repository.LibraryRepository


class NosferatuApp : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val libraryConfig by lazy { LibraryConfig(this) }
    private val coverManager by lazy { CoverManager(this) }

    // Strategies configuration
    private val parser by lazy {
        BookParser(
            strategies = mapOf(
                EbookFormat.EPUB to EpubParser()
                //TODO: EbookFormat.PDF to PdfParser(), and other formats
            )
        )
    }

    private val scanner by lazy { LibraryScanner(parser) }

    val repository by lazy {
        LibraryRepository(
            bookDao = database.bookDao(),
            scanner = scanner,
            coverManager = coverManager,
            libraryConfig = libraryConfig
        )
    }
}