import scala.util.parsing.combinator.RegexParsers

class ASTBuilder extends RegexParsers {
  protected def astNodeParser: Parser[AstNode] = printlnNode | variableDefinition | functionDefinition | variableRedefinition | functionCall | expr | varLoad
  protected def astParser: Parser[ScopeNode] = rep(astNodeParser) ^^ { v => ScopeNode(v) } /*functionDefenition | functionCall | variableDefenition | structDefenition*/
  protected def functionDefinition: Parser[FunctionDefNode] = (("def" ~> ident) ~ (params <~ "{") ~ (astParser <~ "}")) ^^ {p =>
      p._1 match {
        case id ~ ls => FunctionDefNode(id, ls, p._2)
      }}
  protected def functionCall: Parser[FunctionCallNode] = ident ~ commaSeparatedExprs ^^ {p =>
    FunctionCallNode(p._1, p._2)}
  protected def commaSeparatedExprs: Parser[List[AstNode]] = "(" ~> repsep(astNodeParser, ",") <~ ")"
  protected def params: Parser[List[Pair[String, Option[LiteralNode]]]] = "(" ~> repsep(funParamDef | funParam, ",") <~ ")"
  protected def funParam: Parser[Pair[String, Option[LiteralNode]]] = ident ^^ { s => Pair(s, None) }
  protected def funParamDef: Parser[Pair[String, Option[LiteralNode]]] = ident ~ ("=" ~> literalNode) ^^ { p => Pair(p._1, Some(p._2)) }
  protected def varLoad: Parser[LoadVarNode] = ident ^^ {v => LoadVarNode(v) }
  protected def printlnNode: Parser[PrintlnCallNode] = "println" ~> ("(" ~> astNodeParser <~ ")") ^^ { v => PrintlnCallNode(v) }
  protected def literalNode: Parser[LiteralNode] = doubleParser | integerParser | stringLiteralParser
  protected def integerParser: Parser[IntLiteralNode] = integer ^^ { v => IntLiteralNode(BigInt(v)) }
  protected def doubleParser: Parser[DoubleLiteralNode] = double ^^ { v => DoubleLiteralNode(BigDecimal(v)) }
  protected def stringParser: Parser[String] ="""[^"]*""".r
  protected def stringLiteralParser: Parser[StringLiteralNode] = "\"" ~> stringParser <~ "\"" ^^ { v => StringLiteralNode(v) }
  protected def variableDefinition: Parser[DefVarNode] = "var" ~> ident ~ ("=" ~> expr) ^^ {p => DefVarNode(p._1, p._2)
    }
  protected def variableRedefinition: Parser[StoreVarNode] = (ident <~ "=") ~ expr ^^ {p => StoreVarNode(p._1, p._2) }

  def expr: Parser[AstNode] = term ~ rep("+" ~ term | "-" ~ term) ^^ toBinary
  protected def term: Parser[AstNode] = factor ~ rep("*" ~ factor | "/" ~ factor) ^^ toBinary
  protected def factor: Parser[AstNode] = literalNode | varLoad | variableDefinition |
    structDefinition | ("(" ~> expr <~ literal(")"))

  protected def structDefinition: Parser[StructLiteralNode] = ("{" ~> rep(variableDefinition)) <~ "}" ^^ {
    p => StructLiteralNode(p.map(f => (f.name, f.value)).toMap)
  }

  protected def toBinary: PartialFunction[~[AstNode, List[~[String, AstNode]]], AstNode] = {
    case left ~ list =>
      if (list.isEmpty) left
      else list.foldLeft(left) {
        case (res, op ~ right) => BinaryOperationNode(res, op ,right)
      }
  }

  def parse(source: String) = parseAll(astParser, source) match {
    case Success(res, _) => Some(res)
  }

  protected def integer: Parser[String] = """-?0|([1-9]\d*)""".r
  protected def double: Parser[String] = """-?0|([1-9]\d*)\.\d*""".r
  protected def ident: Parser[String] = """[a-zA-Z_][a-zA-Z0-9_]*""".r
}

object ASTBuilder {
  def buildAST(program: String) = (new ASTBuilder).parse(program)
}

