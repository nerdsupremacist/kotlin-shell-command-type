
@file:Command("clang")

import java.io.File

val file = File("testData/test.c")
runBlocking {
    shell {
        clang {
            +E
            +file
        }()
    }
}