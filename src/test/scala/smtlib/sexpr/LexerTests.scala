package smtlib.sexpr

import smtlib.SynchronousPipedReader

import Tokens._

import java.io.{StringReader, PipedInputStream, PipedOutputStream, InputStreamReader, OutputStreamWriter}

import org.scalatest.FunSuite
import org.scalatest.concurrent.Timeouts
import org.scalatest.time.SpanSugar._

class LexerTests extends FunSuite with Timeouts {

  test("eof read") {
    val reader1 = new StringReader("12")
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === IntLit(12))
    assert(lexer1.next === null)
    assert(lexer1.next === null)
  }

  test("integer literals") {
    val reader1 = new StringReader("12")
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === IntLit(12))

    val reader2 = new StringReader("#xF")
    val lexer2 = new Lexer(reader2)
    assert(lexer2.next === IntLit(15))

    val reader3 = new StringReader("#o55")
    val lexer3 = new Lexer(reader3)
    assert(lexer3.next === IntLit(45))

    val reader4 = new StringReader("#x1F")
    val lexer4 = new Lexer(reader4)
    assert(lexer4.next === IntLit(31))

    val reader5 = new StringReader("123 #x11 #o12")
    val lexer5 = new Lexer(reader5)
    assert(lexer5.next === IntLit(123))
    assert(lexer5.next === IntLit(17))
    assert(lexer5.next === IntLit(10))

    val reader6 = new StringReader("#16r21")
    val lexer6 = new Lexer(reader6)
    assert(lexer6.next === IntLit(33))
  }

  test("string literals") {
    val reader1 = new StringReader(""" "12" """)
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === StringLit("12"))

    val reader2 = new StringReader(""" "abc\"def" """)
    val lexer2 = new Lexer(reader2)
    assert(lexer2.next === StringLit("abc\"def"))

    val reader3 = new StringReader(""" " abc \" def" """)
    val lexer3 = new Lexer(reader3)
    assert(lexer3.next === StringLit(" abc \" def"))

    val reader4 = new StringReader(""" "\"abc\"" """)
    val lexer4 = new Lexer(reader4)
    assert(lexer4.next === StringLit("\"abc\""))
  }


  test("symbol literals") {
    val reader1 = new StringReader(""" d12 """)
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === SymbolLit("d12"))

    val reader2 = new StringReader(""" abc\ def """)
    val lexer2 = new Lexer(reader2)
    assert(lexer2.next === SymbolLit("abc def"))

    val reader3 = new StringReader("""  ab\c\ d\ef" """)
    val lexer3 = new Lexer(reader3)
    assert(lexer3.next === SymbolLit("abc def"))

    val reader4 = new StringReader(""" |abc deF| """)
    val lexer4 = new Lexer(reader4)
    assert(lexer4.next === SymbolLit("abc deF"))

    val reader5 = new StringReader(""" 
|abc
deF| 
""")
    val lexer5 = new Lexer(reader5)
    assert(lexer5.next === SymbolLit(
"""abc
deF"""))
  }

  test("qualified symbols") {
    val reader1 = new StringReader(""" :d12 """)
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === QualifiedSymbol(None, "d12"))

    val reader2 = new StringReader(""" abc:def """)
    val lexer2 = new Lexer(reader2)
    assert(lexer2.next === QualifiedSymbol(Some("abc"), "def"))

    val reader3 = new StringReader("""  ab\c:d\ef" """)
    val lexer3 = new Lexer(reader3)
    assert(lexer3.next === QualifiedSymbol(Some("abc"), "def"))

    val reader4 = new StringReader(""" |abc : deF| """)
    val lexer4 = new Lexer(reader4)
    assert(lexer4.next === SymbolLit("abc : deF"))

    val reader5 = new StringReader(""" |abc|:|deF| """)
    val lexer5 = new Lexer(reader5)
    assert(lexer5.next === QualifiedSymbol(Some("abc"), "deF"))
  }

  test("lexer compose") {
    val reader1 = new StringReader("""
      (test "test")
    """)
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === OParen)
    assert(lexer1.next === SymbolLit("test"))
    assert(lexer1.next === StringLit("test"))
    assert(lexer1.next === CParen)


    val reader2 = new StringReader("""
      ) (  42  42.173
    """)
    val lexer2 = new Lexer(reader2)
    assert(lexer2.next === CParen)
    assert(lexer2.next === OParen)
    assert(lexer2.next === IntLit(42))
    assert(lexer2.next === DoubleLit(42.173))

    val reader3 = new StringReader("""
      ) ;(  42  42.173
      12 "salut" ; """)
    val lexer3 = new Lexer(reader3)
    assert(lexer3.next === CParen)
    assert(lexer3.next === IntLit(12))
    assert(lexer3.next === StringLit("salut"))
  }

  test("interactive lexer") {
    val pis = new SynchronousPipedReader
    val lexer = failAfter(3 seconds) { new Lexer(pis) }
    pis.write("12 ")//this is impossible for lexer to determine whether the token is terminated or simply the next char takes time to arrive, so we need some syntactic separation
    assert(lexer.next === IntLit(12))
    pis.write("(")
    assert(lexer.next === OParen)
    pis.write(")")
    assert(lexer.next === CParen)
    pis.write("\"abcd\"")
    assert(lexer.next === StringLit("abcd"))
  }

  /* 
   * Those tests are outaded but were supporting a strict
   * application of the rules of S-Expression symbols in common lisp
   * where the symbol would be converted to upper cases
  test("symbol literals") {
    val reader1 = new StringReader(""" d12 """)
    val lexer1 = new Lexer(reader1)
    assert(lexer1.next === SymbolLit("D12"))

    val reader2 = new StringReader(""" abc\ def """)
    val lexer2 = new Lexer(reader2)
    assert(lexer2.next === SymbolLit("ABC DEF"))

    val reader3 = new StringReader("""  ab\c\ d\ef" """)
    val lexer3 = new Lexer(reader3)
    assert(lexer3.next === SymbolLit("ABc DeF"))

    val reader4 = new StringReader(""" |abc deF| """)
    val lexer4 = new Lexer(reader4)
    assert(lexer4.next === SymbolLit("abc deF"))

    val reader5 = new StringReader(""" 
|abc
deF| 
""")
    val lexer5 = new Lexer(reader5)
    assert(lexer5.next === SymbolLit(
"""abc
deF"""))
  }
  */
}
