/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.dom.lrparser.action.cpp;

import static org.eclipse.cdt.core.parser.util.CollectionUtils.findFirstAndRemove;
import static org.eclipse.cdt.core.parser.util.CollectionUtils.reverseIterable;
import static org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPParsersym.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lpg.lpgjavaruntime.IToken;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConversionName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeleteExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTExplicitTemplateInstantiation;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionTryBlockDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLinkageSpecification;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceAlias;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTOperatorName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTPointerToMember;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTypenameExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDirective;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisiblityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.lrparser.IParser;
import org.eclipse.cdt.core.dom.lrparser.IParserActionTokenProvider;
import org.eclipse.cdt.core.dom.lrparser.LPGTokenAdapter;
import org.eclipse.cdt.core.dom.lrparser.action.BuildASTParserAction;
import org.eclipse.cdt.core.parser.util.DebugUtil;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPExpressionStatementParser;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPNoCastExpressionParser;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPNoFunctionDeclaratorParser;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPParsersym;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPSizeofExpressionParser;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.OverloadableOperator;

/**
 * Semantic actions that build the AST during the parse. 
 * These are the actions that are specific to the C++ parser, the superclass
 * contains actions that can be shared with the C99 parser.
 * 
 * @author Mike Kucera
 */
public class CPPBuildASTParserAction extends BuildASTParserAction {

	
	/** Used to create the AST node objects */
	protected final ICPPASTNodeFactory nodeFactory;
	
	/**
	 * @param parser
	 * @param orderedTerminalSymbols When an instance of this class is created for a parser
	 * that parsers token kinds will be mapped back to the base C99 parser's token kinds.
	 */
	public CPPBuildASTParserAction(ICPPASTNodeFactory nodeFactory, IParserActionTokenProvider parser, IASTTranslationUnit tu) {
		super(nodeFactory, parser, tu);
		this.nodeFactory = nodeFactory;
	}

	@Override 
	protected boolean isCompletionToken(IToken token) {
		return token.getKind() == CPPParsersym.TK_Completion;
	}
	
	
	@Override
	protected IParser getExpressionStatementParser() {
		return new CPPExpressionStatementParser(parser.getOrderedTerminalSymbols()); 
	}
	
	@Override
	protected IParser getNoCastExpressionParser() {
		return new CPPNoCastExpressionParser(parser.getOrderedTerminalSymbols());
	}
	
	
	@Override
	protected IParser getSizeofExpressionParser() {
		return new CPPSizeofExpressionParser(parser.getOrderedTerminalSymbols());
	}
	
	

	/**
	 * new_expression
     *     ::= dcolon_opt 'new' new_placement_opt new_type_id <openscope-ast> new_array_expressions_op new_initializer_opt
     *       | dcolon_opt 'new' new_placement_opt '(' type_id ')' <openscope-ast> new_array_expressions_op new_initializer_opt
	 */
	public void consumeExpressionNew(boolean isNewTypeId) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();

		IASTExpression initializer = (IASTExpression) astStack.pop(); // may be null
		List<Object> arrayExpressions = astStack.closeScope();
		IASTTypeId typeId = (IASTTypeId) astStack.pop();
		IASTExpression placement = (IASTExpression) astStack.pop(); // may be null
		boolean hasDoubleColon = astStack.pop() == PLACE_HOLDER;
		
		ICPPASTNewExpression newExpression = nodeFactory.newCPPNewExpression(placement, initializer, typeId);
		newExpression.setIsGlobal(hasDoubleColon);
		newExpression.setIsNewTypeId(isNewTypeId);
		
		for(Object expr : arrayExpressions)
			newExpression.addNewTypeIdArrayExpression((IASTExpression)expr);
		
		setOffsetAndLength(newExpression);
		astStack.push(newExpression);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * new_declarator -- pointer operators are part of the type id, held in an empty declarator
     *     ::= <openscope-ast> new_pointer_operators
	 */
	public void consumeNewDeclarator() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName name = nodeFactory.newName();
		IASTDeclarator declarator = nodeFactory.newDeclarator(name);
		
		for(Object pointer : astStack.closeScope())
			declarator.addPointerOperator((IASTPointerOperator)pointer);
		
		setOffsetAndLength(declarator);
		astStack.push(declarator);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * throw_expression
     *     ::= 'throw'
     *       | 'throw' assignment_expression
	 */
	public void consumeExpressionThrow(boolean hasExpr) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTExpression operand = hasExpr ? (IASTExpression) astStack.pop() : null;
		IASTUnaryExpression expr = nodeFactory.newUnaryExpression(ICPPASTUnaryExpression.op_throw, operand);
		setOffsetAndLength(expr);
		astStack.push(expr);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * delete_expression
     *     ::= dcolon_opt 'delete' cast_expression
     *       | dcolon_opt 'delete' '[' ']' cast_expression
	 * @param isVectorized
	 */
	public void consumeExpressionDelete(boolean isVectorized) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTExpression operand = (IASTExpression) astStack.pop();
		boolean hasDoubleColon = astStack.pop() == PLACE_HOLDER;
		
		ICPPASTDeleteExpression deleteExpr = nodeFactory.newDeleteExpression(operand);
		deleteExpr.setIsGlobal(hasDoubleColon);
		deleteExpr.setIsVectored(isVectorized);
		
		setOffsetAndLength(deleteExpr);
		astStack.push(deleteExpr);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * 
	 */
	public void consumeExpressionFieldReference(boolean isPointerDereference, boolean hasTemplateKeyword) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName name = (IASTName) astStack.pop();
		IASTExpression owner = (IASTExpression) astStack.pop();
		IASTFieldReference expr = nodeFactory.newFieldReference(name, owner, isPointerDereference, hasTemplateKeyword);
		
		setOffsetAndLength(expr);
		astStack.push(expr);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	/**
	 * postfix_expression
	 *     ::= simple_type_specifier '(' expression_list_opt ')'
	 */
	public void consumeExpressionSimpleTypeConstructor() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTExpression expression = (IASTExpression) astStack.pop();
		IToken token = (IToken) astStack.pop();
		
		int type = asICPPASTSimpleTypeConstructorExpressionType(token);
		ICPPASTSimpleTypeConstructorExpression typeConstructor = nodeFactory.newCPPSimpleTypeConstructorExpression(type, expression);
		
		setOffsetAndLength(typeConstructor);
		astStack.push(typeConstructor);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	private static int asICPPASTSimpleTypeConstructorExpressionType(IToken token) {
		assert token != null;
		
		switch(token.getKind()) {
			case TK_char     : return ICPPASTSimpleTypeConstructorExpression.t_char;
			case TK_wchar_t  : return ICPPASTSimpleTypeConstructorExpression.t_wchar_t;
			case TK_bool     : return ICPPASTSimpleTypeConstructorExpression.t_bool;
			case TK_short    : return ICPPASTSimpleTypeConstructorExpression.t_short;
			case TK_int      : return ICPPASTSimpleTypeConstructorExpression.t_int;
			case TK_long     : return ICPPASTSimpleTypeConstructorExpression.t_long;
			case TK_signed   : return ICPPASTSimpleTypeConstructorExpression.t_signed;
			case TK_unsigned : return ICPPASTSimpleTypeConstructorExpression.t_unsigned;
			case TK_float    : return ICPPASTSimpleTypeConstructorExpression.t_float;
			case TK_double   : return ICPPASTSimpleTypeConstructorExpression.t_double;
			case TK_void     : return ICPPASTSimpleTypeConstructorExpression.t_void;
		
			default:
				assert false : "type parsed wrong"; //$NON-NLS-1$
				return ICPPASTSimpleTypeConstructorExpression.t_unspecified;
		}
	}
	
	
	/**
	 * postfix_expression
	 *     ::= 'typename' dcolon_opt nested_name_specifier <empty>  identifier_name '(' expression_list_opt ')'
     *       | 'typename' dcolon_opt nested_name_specifier template_opt template_id '(' expression_list_opt ')'
	 */
	@SuppressWarnings("unchecked")
	public void consumeExpressionTypeName() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTExpression expr = (IASTExpression) astStack.pop();
		IASTName name = (IASTName) astStack.pop();
		boolean isTemplate = astStack.pop() == PLACE_HOLDER;
		LinkedList<IASTName> nestedNames = (LinkedList<IASTName>) astStack.pop();
		boolean hasDColon = astStack.pop() == PLACE_HOLDER;
		
		nestedNames.addFirst(name);
		IASTName qualifiedName = createQualifiedName(nestedNames, hasDColon);
		
		ICPPASTTypenameExpression typenameExpr = nodeFactory.newCPPTypenameExpression(qualifiedName, expr, isTemplate);
		
		setOffsetAndLength(typenameExpr);
		astStack.push(typenameExpr);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * condition
     *     ::= type_specifier_seq declarator '=' assignment_expression
	 */
	public void consumeConditionDeclaration() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTExpression expr = (IASTExpression) astStack.pop();
		IASTDeclarator declarator = (IASTDeclarator) astStack.pop();
		IASTDeclSpecifier declSpec = (IASTDeclSpecifier) astStack.pop();
		
		IASTInitializerExpression initializer = nodeFactory.newInitializerExpression(expr);
		setOffsetAndLength(initializer, offset(expr), length(expr));
		declarator.setInitializer(initializer);
		
		IASTSimpleDeclaration declaration = nodeFactory.newSimpleDeclaration(declSpec);
		declaration.addDeclarator(declarator);
		
		setOffsetAndLength(declaration);
		astStack.push(declaration);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * template_id
     *     ::= identifier_name '<' <openscope-ast> template_argument_list_opt '>'
     *     
     * operator_function_id
     *     ::= operator_id '<' <openscope-ast> template_argument_list_opt '>'
	 */
	public void consumeTemplateId() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> templateArguments = astStack.closeScope();
		IASTName name = (IASTName) astStack.pop();
		
		ICPPASTTemplateId templateId = nodeFactory.newCPPTemplateId(name);
		
		for(Object arg : templateArguments) {
			if(arg instanceof IASTExpression)
				templateId.addTemplateArgument((IASTExpression)arg);
			else
				templateId.addTemplateArgument((IASTTypeId)arg);
		}
		
		setOffsetAndLength(templateId);
		astStack.push(templateId);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	

	/**
	 * operator_id
     *     ::= 'operator' overloadable_operator
	 */
	public void consumeOperatorName() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<IToken> tokens = parser.getRuleTokens();
		tokens = tokens.subList(1, tokens.size());
		OverloadableOperator operator = getOverloadableOperator(tokens);
		
		ICPPASTOperatorName name = nodeFactory.newCPPOperatorName(operator);
		setOffsetAndLength(name);
		astStack.push(name); 
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}

	
	@SuppressWarnings("restriction")
	private static OverloadableOperator getOverloadableOperator(List<IToken> tokens) {
		if(tokens.size() == 1) {
			// TODO this is a hack that I did to save time
			LPGTokenAdapter coreToken = (LPGTokenAdapter) tokens.get(0);
			return OverloadableOperator.valueOf(coreToken.getWrappedToken());
		}
		else if(matchTokens(tokens, TK_new, TK_LeftBracket, TK_RightBracket)) {
			return OverloadableOperator.NEW_ARRAY;
		}
		else if(matchTokens(tokens, TK_delete, TK_LeftBracket, TK_RightBracket)) {
			return OverloadableOperator.DELETE_ARRAY;
		}
		else if(matchTokens(tokens, TK_LeftBracket, TK_RightBracket)) {
			return OverloadableOperator.BRACKET;
		}
		else if(matchTokens(tokens, TK_LeftParen, TK_RightParen)) {
			return OverloadableOperator.PAREN;
		}
		
		return null;
	}
	
	
	/**
	 * conversion_function_id
     *     ::= 'operator' conversion_type_id
	 */
	public void consumeConversionName() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		char[] chars = tokenListToNameCharArray(parser.getRuleTokens());
		IASTTypeId typeId = (IASTTypeId) astStack.pop();
		ICPPASTConversionName name = nodeFactory.newCPPConversionName(chars, typeId);
		setOffsetAndLength(name);
		astStack.push(name);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	
	
    /**
     * unqualified_id
     *     ::= '~' identifier_token
     */
  	public void consumeDestructorName() {
  		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
  		
  		// concatenate the '~' with the identifier token
  		char[] chars = tokenListToNameCharArray(parser.getRuleTokens());
  		
  		IASTName name = nodeFactory.newName(chars);
  		setOffsetAndLength(name);
  		astStack.push(name);
  		
  		if(TRACE_AST_STACK) System.out.println(astStack);
  	}
  	
  	
  	/**
  	 * destructor_type_name
     *     ::= '~' template_id_name
  	 */
  	public void consumeDestructorNameTemplateId() {
  		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
  		
  		ICPPASTTemplateId templateId = (ICPPASTTemplateId) astStack.peek();
  		
  		IASTName oldName = templateId.getTemplateName();
  		char[] newChars = ("~" + oldName).toCharArray(); //$NON-NLS-1$
  		
  		IASTName newName = nodeFactory.newName(newChars);
  		
  		int offset = offset(parser.getLeftIToken());
  		int length = offset - endOffset(oldName);
  		setOffsetAndLength(newName, offset, length);
  		
  		templateId.setTemplateName(newName);
  		
  		if(TRACE_AST_STACK) System.out.println(astStack);
  	}
  	
  	
  	
  	/**
  	 * qualified_id
     *     ::= '::' identifier_name
     *       | '::' operator_function_id
     *       | '::' template_id
  	 */
  	public void consumeGlobalQualifiedId() {
  		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
  		
  		IASTName name = (IASTName) astStack.pop();
  		
  		ICPPASTQualifiedName qualifiedName = nodeFactory.newCPPQualifiedName();
  		qualifiedName.addName(name);
  		qualifiedName.setFullyQualified(true);
  		
  		setOffsetAndLength(qualifiedName);
  		astStack.push(name);
  		
  		if(TRACE_AST_STACK) System.out.println(astStack);
  	}
  	
  	
  	/**
	 * selection_statement ::=  switch '(' condition ')' statement
	 */
	public void consumeStatementSwitch() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTStatement body  = (IASTStatement)  astStack.pop();
		
		Object condition = astStack.pop();
		
		IASTSwitchStatement stat;
		if(condition instanceof IASTExpression)
			stat = nodeFactory.newSwitchStatment((IASTExpression)condition, body);
		else
			stat = nodeFactory.newSwitchStatment((IASTDeclaration)condition, body);
		
		
		setOffsetAndLength(stat);
		astStack.push(stat);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	

	public void consumeStatementIf(boolean hasElse) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTStatement elseClause = hasElse ? (IASTStatement)astStack.pop() : null;		
		IASTStatement thenClause = (IASTStatement) astStack.pop();
		
		Object condition = astStack.pop();
		
		IASTIfStatement ifStatement;
		if(condition instanceof IASTExpression)
			ifStatement = nodeFactory.newIfStatement((IASTExpression)condition, thenClause, elseClause);
		else
			ifStatement = nodeFactory.newIfStatement((IASTDeclaration)condition, thenClause, elseClause);
		
		setOffsetAndLength(ifStatement);
		astStack.push(ifStatement);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * iteration_statement ::= 'while' '(' condition ')' statement
	 */
	public void consumeStatementWhileLoop() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTStatement body = (IASTStatement) astStack.pop();
		
		Object condition = astStack.pop();
		
		IASTWhileStatement whileStatement;
		if(condition instanceof IASTExpression)
			whileStatement = nodeFactory.newWhileStatement((IASTExpression)condition, body);
		else
			whileStatement = nodeFactory.newWhileStatement((IASTDeclaration)condition, body);
		
		setOffsetAndLength(whileStatement);
		astStack.push(whileStatement);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * try_block
     *     ::= 'try' compound_statement <openscope-ast> handler_seq
     */
	public void consumeStatementTryBlock() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> handlerSeq = astStack.closeScope();
		IASTStatement body = (IASTStatement) astStack.pop();
		
		ICPPASTTryBlockStatement tryStatement = nodeFactory.newTryBlockStatement(body);
		
		for(Object handler : handlerSeq)
			tryStatement.addCatchHandler((ICPPASTCatchHandler)handler);
		
		setOffsetAndLength(tryStatement);
		astStack.push(tryStatement);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * handler
     *     ::= 'catch' '(' exception_declaration ')' compound_statement
     *       | 'catch' '(' '...' ')' compound_statement
	 */
	 public void consumeStatementCatchHandler(boolean hasEllipsis) {
		 if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		 
		 IASTStatement body = (IASTStatement) astStack.pop();
		 IASTDeclaration decl = hasEllipsis ? null : (IASTDeclaration) astStack.pop();
		 
		 ICPPASTCatchHandler catchHandler = nodeFactory.newCatchHandler(decl, body);
		 catchHandler.setIsCatchAll(hasEllipsis);

		 setOffsetAndLength(catchHandler);
	     astStack.push(catchHandler);
		 
		 if(TRACE_AST_STACK) System.out.println(astStack);
	 }
	 
	
	/**
	 * nested_name_specifier
     *     ::= class_or_namespace_name '::' nested_name_specifier_with_template
     *       | class_or_namespace_name '::' 
     *
     * nested_name_specifier_with_template
     *     ::= class_or_namespace_name_with_template '::' nested_name_specifier_with_template
     *       | class_or_namespace_name_with_template '::'
     *       
     *        
     * Creates and updates a list of the nested names on the stack.
     * Important: the names in the list are in *reverse* order,
     * this is because the actions fire in reverse order.
	 */
	@SuppressWarnings("unchecked")
	public void consumeNestedNameSpecifier(final boolean hasNested) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		LinkedList<IASTName> names;
		if(hasNested)
			names = (LinkedList<IASTName>) astStack.pop();
		else
			names = new LinkedList<IASTName>();
		
		IASTName name = (IASTName) astStack.pop();
		names.add(name);
		
		astStack.push(names);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}

	
	public void consumeNestedNameSpecifierEmpty() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		// can't use Collections.EMPTY_LIST because we need a list thats mutable
		astStack.push(new LinkedList<IASTName>());
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	
	
	/**
	 * The template keyword is optional but must be the leftmost token.
	 */
	@Deprecated public void consumeNameWithTemplateKeyword() { 
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName name = (IASTName) astStack.pop();
		boolean hasTemplateKeyword = astStack.pop() == PLACE_HOLDER;
		
		if(hasTemplateKeyword)
			name = addTemplateKeyword(parser.getLeftIToken(), name);
		
		astStack.push(name);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	@Deprecated private IASTName addTemplateKeyword(final IToken templateKeyword, final IASTName name) {
		IASTName replacement = nodeFactory.newName((templateKeyword + " " + name).toCharArray()); //$NON-NLS-1$
		int offset = offset(templateKeyword);
		int length = length(name) + (offset(name) - offset); 
		setOffsetAndLength(replacement, offset, length);
		return replacement;
	}
	
	
	
	/**
	 * qualified_id
     *     ::= dcolon_opt nested_name_specifier any_name
	 */
	public void consumeQualifiedId(boolean hasTemplateKeyword) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();

		IASTName qualifiedName = subRuleQualifiedName(hasTemplateKeyword);
		astStack.push(qualifiedName);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	
	
	
	/**
	 * Creates a qualified name from a list of names (that must be in reverse order).
	 */
	@SuppressWarnings("restriction")
	private IASTName createQualifiedName(LinkedList<IASTName> nestedNames, boolean startsWithColonColon) {
		if(!startsWithColonColon && nestedNames.size() == 1) // its actually an unqualified name
			return nestedNames.get(0);
		
		int startOffset = offset(nestedNames.getLast());
		int endOffset   = endOffset(nestedNames.getFirst());
		int length      = endOffset - startOffset;
	
		// find the tokens that make up the name
		List<IToken> nameTokens = tokenOffsetSubList(parser.getRuleTokens(), startOffset, endOffset);
		
		StringBuilder signature = new StringBuilder(startsWithColonColon ? "::" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		IToken prev = null;
		for(IToken t : nameTokens) {
			if(needSpaceBetween(prev, t))
				signature.append(' ');
			signature.append(t.toString());
			prev = t;
		}
		
		ICPPASTQualifiedName qualifiedName = nodeFactory.newCPPQualifiedName();
		qualifiedName.setFullyQualified(startsWithColonColon);
		setOffsetAndLength(qualifiedName, startOffset, length);
		((CPPASTQualifiedName)qualifiedName).setSignature(signature.toString());
		
		for(IASTName name : reverseIterable(nestedNames))
			qualifiedName.addName(name);
				
		return qualifiedName;
	}
	
	
	private static boolean needSpaceBetween(IToken prev, IToken iter) {
		if(prev == null)
			return false;
		
		int prevKind = prev.getKind();
		int iterKind = iter.getKind();
		
		return  prevKind != TK_ColonColon && 
				prevKind != TK_identifier && 
				prevKind != TK_LT &&
				prevKind != TK_Tilde &&
				iterKind != TK_GT && 
				prevKind != TK_LeftBracket && 
				iterKind != TK_RightBracket && 
				iterKind != TK_ColonColon;
	}
	
	/**
	 * Consumes grammar sub-rules of the following form:
	 * 
	 * dcolon_opt nested_name_specifier_opt keyword_opt name
	 * 
	 * Where name is any rule that produces an IASTName node on the stack.
	 * Does not place the resulting node on the stack, returns it instead.
	 */
	@SuppressWarnings("unchecked")
	private IASTName subRuleQualifiedName(boolean hasOptionalKeyword) {
		IASTName lastName = (IASTName) astStack.pop();
		
		if(hasOptionalKeyword) // this is usually a template keyword and can be ignored
			astStack.pop();
		
		LinkedList<IASTName> nestedNames = (LinkedList<IASTName>) astStack.pop();
		boolean startsWithColonColon = astStack.pop() == PLACE_HOLDER;
		
		if(nestedNames.isEmpty() && !startsWithColonColon) { // then its not a qualified name
			return lastName;
		}

		nestedNames.addFirst(lastName); // the list of names is in reverse order
		
		return createQualifiedName(nestedNames, startsWithColonColon);
	}
	
	
	
	/**
	 * pseudo_destructor_name
     *     ::= dcolon_opt nested_name_specifier_opt type_name '::' destructor_type_name
     *       | dcolon_opt nested_name_specifier 'template' template_id '::' destructor_type_name
     *       | dcolon_opt nested_name_specifier_opt destructor_type_name
     */
	@SuppressWarnings("unchecked")
	public void consumePsudoDestructorName(boolean hasExtraTypeName) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName destructorTypeName = (IASTName) astStack.pop();
		IASTName extraName = hasExtraTypeName ? (IASTName) astStack.pop() : null;
		LinkedList<IASTName> nestedNames = (LinkedList<IASTName>) astStack.pop();
		boolean hasDColon = astStack.pop() == PLACE_HOLDER;
		
		if(hasExtraTypeName)
			nestedNames.addFirst(extraName);
		
		nestedNames.addFirst(destructorTypeName);
		
		IASTName qualifiedName = createQualifiedName(nestedNames, hasDColon);
		
		setOffsetAndLength(qualifiedName);
		astStack.push(qualifiedName);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * asm_definition
     *     ::= 'asm' '(' 'stringlit' ')' ';'
	 */
	public void consumeDeclarationASM() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		String s = parser.getRuleTokens().get(2).toString();
		IASTASMDeclaration asm = nodeFactory.newASMDeclaration(s);
		
		setOffsetAndLength(asm);
		astStack.push(asm);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	/**
	 * namespace_alias_definition
     *     ::= 'namespace' 'identifier' '=' dcolon_opt nested_name_specifier_opt namespace_name ';'
     */
	public void consumeNamespaceAliasDefinition() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();

		IASTName qualifiedName = subRuleQualifiedName(false);
		
		IASTName alias = createName(parser.getRuleTokens().get(1));
		ICPPASTNamespaceAlias namespaceAlias = nodeFactory.newNamespaceAlias(alias, qualifiedName);
		
		setOffsetAndLength(namespaceAlias);
		astStack.push(namespaceAlias);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * using_declaration
     *     ::= 'using' typename_opt dcolon_opt nested_name_specifier_opt unqualified_id ';'
	 */
	public void consumeUsingDeclaration() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName qualifiedName = subRuleQualifiedName(false);
		boolean hasTypenameKeyword = astStack.pop() == PLACE_HOLDER;
		
		ICPPASTUsingDeclaration usingDeclaration = nodeFactory.newUsingDeclaration(qualifiedName);
		usingDeclaration.setIsTypename(hasTypenameKeyword);
		
		setOffsetAndLength(usingDeclaration);
		astStack.push(usingDeclaration);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * using_directive
     *     ::= 'using' 'namespace' dcolon_opt nested_name_specifier_opt namespace_name ';'
	 */
	public void consumeUsingDirective() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName qualifiedName = subRuleQualifiedName(false);
		
		ICPPASTUsingDirective usingDirective = nodeFactory.newUsingDirective(qualifiedName);
		setOffsetAndLength(usingDirective);
		astStack.push(usingDirective);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * linkage_specification
     *     ::= 'extern' 'stringlit' '{' <openscope-ast> declaration_seq_opt '}'
     *       | 'extern' 'stringlit' <openscope-ast> declaration
	 */
	public void consumeLinkageSpecification() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		String name = parser.getRuleTokens().get(1).toString();
		ICPPASTLinkageSpecification linkageSpec = nodeFactory.newLinkageSpecification(name);
		
		for(Object declaration : astStack.closeScope())
			linkageSpec.addDeclaration((IASTDeclaration)declaration);
			
		setOffsetAndLength(linkageSpec);
		astStack.push(linkageSpec);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	
	/**
	 * original_namespace_definition
     *     ::= 'namespace' identifier_name '{' <openscope-ast> declaration_seq_opt '}'
     *    
     * extension_namespace_definition
     *     ::= 'namespace' original_namespace_name '{' <openscope-ast> declaration_seq_opt '}'
     *
     * unnamed_namespace_definition
     *     ::= 'namespace' '{' <openscope-ast> declaration_seq_opt '}'
	 */
	public void consumeNamespaceDefinition(boolean hasName) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> declarations = astStack.closeScope();
		IASTName namespaceName = hasName ? (IASTName)astStack.pop() : nodeFactory.newName();
		
		ICPPASTNamespaceDefinition definition = nodeFactory.newNamespaceDefinition(namespaceName);
		
		for(Object declaration : declarations)
			definition.addDeclaration((IASTDeclaration)declaration);
		
		setOffsetAndLength(definition);
		astStack.push(definition);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * template_declaration
     *     ::= export_opt 'template' '<' <openscope-ast> template_parameter_list '>' declaration
	 */
	public void consumeTemplateDeclaration() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTDeclaration declaration = (IASTDeclaration) astStack.pop();
		
		ICPPASTTemplateDeclaration templateDeclaration = nodeFactory.newTemplateDeclaration(declaration);
		
		for(Object param : astStack.closeScope())
			templateDeclaration.addTemplateParamter((ICPPASTTemplateParameter)param);

		boolean hasExportKeyword = astStack.pop() == PLACE_HOLDER;
		templateDeclaration.setExported(hasExportKeyword);
		
		setOffsetAndLength(templateDeclaration);
		astStack.push(templateDeclaration);

		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
    /**
     * explicit_instantiation
     *    ::= 'template' declaration
     */
	public void consumeTemplateExplicitInstantiation() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTDeclaration declaration = (IASTDeclaration) astStack.pop();
		ICPPASTExplicitTemplateInstantiation instantiation = nodeFactory.newExplicitTemplateInstantiation(declaration);
		
		setOffsetAndLength(instantiation);
		astStack.push(instantiation);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * explicit_specialization
     *     ::= 'template' '<' '>' declaration
     */
	public void consumeTemplateExplicitSpecialization() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTDeclaration declaration = (IASTDeclaration) astStack.pop();
		ICPPASTTemplateSpecialization specialization = nodeFactory.newTemplateSpecialization(declaration);
		
		setOffsetAndLength(specialization);
		astStack.push(specialization);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * Sets a token specifier.
	 * Needs to be overrideable for new decl spec keywords.
	 * 
	 * @param token Allows subclasses to override this method and use any
	 * object to determine how to set a specifier.
	 */
	protected void setSpecifier(IASTDeclSpecifier node, IToken token) {
		//TODO int kind = asC99Kind(token)
		int kind = token.getKind();
		switch(kind){
			case TK_typedef:  node.setStorageClass(IASTDeclSpecifier.sc_typedef);    return;
			case TK_extern:   node.setStorageClass(IASTDeclSpecifier.sc_extern);     return;
			case TK_static:   node.setStorageClass(IASTDeclSpecifier.sc_static);     return;
			case TK_auto:     node.setStorageClass(IASTDeclSpecifier.sc_auto);       return;
			case TK_register: node.setStorageClass(IASTDeclSpecifier.sc_register);   return;
			case TK_mutable:  node.setStorageClass(ICPPASTDeclSpecifier.sc_mutable); return;
			
			case TK_inline:   node.setInline(true);   return;
			case TK_const:    node.setConst(true);    return;
			case TK_volatile: node.setVolatile(true); return;
		}
		
		if(node instanceof ICPPASTSimpleDeclSpecifier) {
			ICPPASTSimpleDeclSpecifier n = (ICPPASTSimpleDeclSpecifier) node;
			switch(kind) {
				case TK_void:     n.setType(IASTSimpleDeclSpecifier.t_void);       break;
				case TK_char:     n.setType(IASTSimpleDeclSpecifier.t_char);       break;
				case TK_int:      n.setType(IASTSimpleDeclSpecifier.t_int);        break;
				case TK_float:    n.setType(IASTSimpleDeclSpecifier.t_float);      break;
				case TK_double:   n.setType(IASTSimpleDeclSpecifier.t_double);     break;
				case TK_bool:     n.setType(ICPPASTSimpleDeclSpecifier.t_bool);    break;
				case TK_wchar_t:  n.setType(ICPPASTSimpleDeclSpecifier.t_wchar_t); break;
				
				case TK_signed:   n.setSigned(true);   break;
				case TK_unsigned: n.setUnsigned(true); break;
				case TK_long:     n.setLong(true);     break;
				case TK_short:    n.setShort(true);    break;
				case TK_friend:   n.setFriend(true);   break;
				case TK_virtual:  n.setVirtual(true);  break;
				case TK_volatile: n.setVolatile(true); break;
				case TK_explicit: n.setExplicit(true); break;
			}
		}
	}
	
	
	public void consumeDeclarationSpecifiersSimple() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTSimpleDeclSpecifier declSpec = nodeFactory.newCPPSimpleDeclSpecifier();
		
		for(Object token : astStack.closeScope())
			setSpecifier(declSpec, (IToken)token);
		
		setOffsetAndLength(declSpec);
		astStack.push(declSpec);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}

	
	/**
	 * TODO: maybe move this into the superclass
	 */
	public void consumeDeclarationSpecifiersComposite() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> topScope = astStack.closeScope();
		
		// There's already a composite or elaborated or enum type specifier somewhere on the stack, find it.
		IASTDeclSpecifier declSpec = findFirstAndRemove(topScope, IASTDeclSpecifier.class);
		
		// now apply the rest of the specifiers
		for(Object token : topScope)
			setSpecifier(declSpec, (IToken)token);
		
		setOffsetAndLength(declSpec);
		astStack.push(declSpec);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	
	
//	/**
//	 * declaration_specifiers ::=  <openscope> type_name_declaration_specifiers
//	 */
	public void consumeDeclarationSpecifiersTypeName() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> topScope = astStack.closeScope();
		// There's a name somewhere on the stack, find it		
		IASTName typeName = findFirstAndRemove(topScope, IASTName.class);
		
		// TODO what does the second argument mean?
		ICPPASTNamedTypeSpecifier declSpec = nodeFactory.newCPPNamedTypeSpecifier(typeName, true);
		
		// now apply the rest of the specifiers
		for(Object token : topScope)
			setSpecifier(declSpec, (IToken)token);

		setOffsetAndLength(declSpec);
		astStack.push(declSpec);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * elaborated_type_specifier
     *     ::= class_keyword dcolon_opt nested_name_specifier_opt identifier_name
     *       | class_keyword dcolon_opt nested_name_specifier_opt template_opt template_id_name
     *       | 'enum' dcolon_opt nested_name_specifier_opt identifier_name      
	 */
	public void consumeTypeSpecifierElaborated(boolean hasOptionalTemplateKeyword) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName name = subRuleQualifiedName(hasOptionalTemplateKeyword);
		int kind = getElaboratedTypeSpecifier(parser.getLeftIToken());
		
		IASTElaboratedTypeSpecifier typeSpecifier = nodeFactory.newElaboratedTypeSpecifier(kind, name);
		
		setOffsetAndLength(typeSpecifier);
		astStack.push(typeSpecifier);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	private static int getElaboratedTypeSpecifier(IToken token) {
		int kind = token.getKind();
		switch(kind) {
			default: assert false : "wrong token kind: " + kind; //$NON-NLS-1$
			case TK_struct: return IASTElaboratedTypeSpecifier.k_struct;
			case TK_union:  return IASTElaboratedTypeSpecifier.k_union;
			case TK_enum:   return IASTElaboratedTypeSpecifier.k_enum;
			case TK_class:  return ICPPASTElaboratedTypeSpecifier.k_class;   
		}
	}
	
	
	
	/**
	 * simple_declaration
     *     ::= declaration_specifiers_opt <openscope-ast> init_declarator_list_opt ';'
     *     
     * TODO: remove attemptAmbiguityResolution parameter
	 */
	public void consumeDeclarationSimple(boolean hasDeclaratorList, boolean attemptAmbiguityResolution) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> declarators = (hasDeclaratorList) ? astStack.closeScope() : Collections.emptyList();
		IASTDeclSpecifier declSpecifier = (IASTDeclSpecifier) astStack.pop(); // may be null
		
		// do not generate nodes for extra EOC tokens
		if(matchTokens(parser.getRuleTokens(), CPPParsersym.TK_EndOfCompletion))
			return;

		if(declSpecifier == null) { // can happen if implicit int is used
			declSpecifier = nodeFactory.newSimpleDeclSpecifier();
			setOffsetAndLength(declSpecifier, parser.getLeftIToken().getStartOffset(), 0);
		}

		IASTSimpleDeclaration declaration = nodeFactory.newSimpleDeclaration(declSpecifier);
		setOffsetAndLength(declaration);
		for(Object declarator : declarators)
			declaration.addDeclarator((IASTDeclarator)declarator);
		
		astStack.push(declaration);

//		IASTNode alternateDeclaration = null;
//		if(attemptAmbiguityResolution) {// && hasConstructorInitializer(declaration)) { // try to resolve the constructor initializer ambiguity
//			// TODO should this ambiguity be resolved here, or at the level of individual declarators?
//			IParser alternateParser = new CPPNoConstructorInitializerParser(parser.getOrderedTerminalSymbols()); 
//			alternateDeclaration = runSecondaryParser(alternateParser);
//		}
//		
//		if(alternateDeclaration == null || alternateDeclaration instanceof IASTProblemDeclaration)
//			astStack.push(declaration);
//		else {
//			System.out.println("Ambiguous Declaration in my parser!");
////	        ASTPrinter.print(declaration);
////	        System.out.println();
////	        ASTPrinter.print(alternateDeclaration);
////	        System.out.println();
//
//	        IASTNode ambiguityNode = nodeFactory.newAmbiguousDeclaration((IASTDeclaration)alternateDeclaration, declaration);
//	        setOffsetAndLength(ambiguityNode);
//			astStack.push(ambiguityNode);
//		}
//		
		
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	public void consumeInitDeclaratorComplete() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTDeclarator declarator = (IASTDeclarator) astStack.peek();
		if(!(declarator instanceof IASTFunctionDeclarator))
			return;
		
		IParser alternateParser = new CPPNoFunctionDeclaratorParser(parser.getOrderedTerminalSymbols()); 
		IASTNode alternateDeclarator = runSecondaryParser(alternateParser);
	
		if(alternateDeclarator == null  || alternateDeclarator instanceof IASTProblemDeclaration)
			return;
		
		astStack.pop();
		IASTNode ambiguityNode = new CPPASTAmbiguousDeclarator(declarator, (IASTDeclarator)alternateDeclarator);
				
        setOffsetAndLength(ambiguityNode);
		astStack.push(ambiguityNode);
        
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	
	/**
	 * Returns true iff the given AST contains at least one constructor initializer node.
	 * Can be called on any AST node but is mean to be called on declarations or declarators.
	 * 
	 * TODO how freaking inefficient is this?
	 */
	private static boolean hasConstructorInitializer(IASTNode declaration) {
		final boolean[] found = {false}; 
		
		ASTVisitor detector = new ASTVisitor() {
			{shouldVisitInitializers = true;}
			@Override
			public int visit(IASTInitializer initializer) {
				if(initializer instanceof ICPPASTConstructorInitializer) {
					found[0] = true; // who said Java doesn't have closures
					return PROCESS_ABORT;
				}
				return PROCESS_CONTINUE;
			}
		};
		
		declaration.accept(detector);
		System.out.println("hasConstructorInitializer: " + found[0]);
		return found[0];
	}
	
	
	/**
	 * visibility_label
     *     ::= access_specifier_keyword ':'
	 */
	public void consumeVisibilityLabel() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();

		IToken specifier = (IToken)astStack.pop();
		int visibility = getAccessSpecifier(specifier);
		ICPPASTVisiblityLabel visibilityLabel = nodeFactory.newVisibilityLabel(visibility);
		setOffsetAndLength(visibilityLabel);
		astStack.push(visibilityLabel);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	private static int getAccessSpecifier(IToken token) {
		int kind = token.getKind();
		switch(kind) {
			default: assert false : "wrong token kind: " + kind; //$NON-NLS-1$
			case TK_private:   return ICPPASTVisiblityLabel.v_private;
			case TK_public:    return ICPPASTVisiblityLabel.v_public;    
			case TK_protected: return ICPPASTVisiblityLabel.v_protected;
		}
	}
	
	
	/**
	 * base_specifier
     *     ::= dcolon_opt nested_name_specifier_opt class_name
     *       | 'virtual' access_specifier_keyword_opt dcolon_opt nested_name_specifier_opt class_name
     *       | access_specifier_keyword 'virtual' dcolon_opt nested_name_specifier_opt class_name
     *       | access_specifier_keyword dcolon_opt nested_name_specifier_opt class_name
	 */
	public void consumeBaseSpecifier(boolean hasAccessSpecifier, boolean isVirtual) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		IASTName name = subRuleQualifiedName(false);
		
		int visibility = 0; // this is the default value that the DOM parser uses
		if(hasAccessSpecifier) {
			IToken accessSpecifierToken = (IToken) astStack.pop();
			if(accessSpecifierToken != null)
				visibility = getAccessSpecifier(accessSpecifierToken);
		}
		
		ICPPASTBaseSpecifier baseSpecifier = nodeFactory.newBaseSpecifier(name, visibility, isVirtual);
		setOffsetAndLength(baseSpecifier);
		astStack.push(baseSpecifier);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
		
	
	/**
	 * Gets the current token and places it on the stack for later consumption.
	 */
	public void consumeAccessKeywordToken() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		astStack.push(parser.getRightIToken());
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	/**
	 * class_specifier
     *     ::= class_head '{' <openscope-ast> member_declaration_list_opt '}'
     */
	public void consumeClassSpecifier() {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		List<Object> declarations = astStack.closeScope();
		
		// the class specifier is created by the rule for class_head
		IASTCompositeTypeSpecifier classSpecifier = (IASTCompositeTypeSpecifier) astStack.pop();
		
		for(Object declaration : declarations) {
			classSpecifier.addMemberDeclaration((IASTDeclaration)declaration);
		}
		
		setOffsetAndLength(classSpecifier);
		astStack.push(classSpecifier);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	/**
     * class_head
     *     ::= class_keyword identifier_name_opt <openscope-ast> base_clause_opt
     *       | class_keyword template_id <openscope-ast> base_clause_opt
     *       | class_keyword nested_name_specifier identifier_name <openscope-ast> base_clause_opt
     *       | class_keyword nested_name_specifier template_id <openscope-ast> base_clause_opt
	 */
	@SuppressWarnings("unchecked")
	public void consumeClassHead(boolean hasNestedNameSpecifier) {
		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
		
		int key = getCompositeTypeSpecifier(parser.getLeftIToken());
		List<Object> baseSpecifiers = astStack.closeScope();
		
		// may be null, but if it is then hasNestedNameSpecifier == false
		IASTName className = (IASTName) astStack.pop();
		
		if(hasNestedNameSpecifier) {
			LinkedList<IASTName> nestedNames = (LinkedList<IASTName>) astStack.pop();
			nestedNames.addFirst(className);
			className = createQualifiedName(nestedNames, false);
		}

		if(className == null)
			className = nodeFactory.newName();
		
		ICPPASTCompositeTypeSpecifier classSpecifier = nodeFactory.newCPPCompositeTypeSpecifier(key, className);
		
		for(Object base : baseSpecifiers)
			classSpecifier.addBaseSpecifier((ICPPASTBaseSpecifier)base);
		
		// the offset and length are set in consumeClassSpecifier()
		astStack.push(classSpecifier);
		
		if(TRACE_AST_STACK) System.out.println(astStack);
	}
	
	
	private static int getCompositeTypeSpecifier(IToken token) {
		int kind = token.getKind();
		switch(kind) {
			default: assert false : "wrong token kind: " + kind; //$NON-NLS-1$
			case TK_struct: return IASTCompositeTypeSpecifier.k_struct;
			case TK_union:  return IASTCompositeTypeSpecifier.k_union;
			case TK_class:  return ICPPASTCompositeTypeSpecifier.k_class;   
		}
	}
	
	
	/**
	 * ptr_operator
     *     ::= '*' <openscope-ast> cv_qualifier_seq_opt
     */
    public void consumePointer() {
    	if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	
    	IASTPointer pointer = nodeFactory.newCPPPointer();
    	List<Object> tokens = astStack.closeScope();
    	addCVQualifiersToPointer(pointer, tokens);
		setOffsetAndLength(pointer);
		astStack.push(pointer);
    	
    	if(TRACE_AST_STACK) System.out.println(astStack);
    }
    
    
    private static void addCVQualifiersToPointer(IASTPointer pointer, List<Object> tokens) {
    	for(Object t : tokens) {
    		IToken token = (IToken) t;
			int kind = token.getKind(); // TODO this should be asXXXKind
			switch(kind) {
				default : assert false;
				case TK_const:    pointer.setConst(true);    break;
				case TK_volatile: pointer.setVolatile(true); break;
			}
		}
    }
    
    /**
	 * ptr_operator
     *     ::= '&'
     */ 
    public void consumeReferenceOperator() {
        if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	
        ICPPASTReferenceOperator referenceOperator = nodeFactory.newReferenceOperator();
        setOffsetAndLength(referenceOperator);
		astStack.push(referenceOperator);
		
    	if(TRACE_AST_STACK) System.out.println(astStack);
    }
    
    
    /**
	 * ptr_operator
     *     ::= dcolon_opt nested_name_specifier '*' <openscope-ast> cv_qualifier_seq_opt
     */
     @SuppressWarnings("unchecked")
	public void consumePointerToMember() {
    	 if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
     	
    	 List<Object> qualifiers = astStack.closeScope();
    	 
    	 LinkedList<IASTName> nestedNames = (LinkedList<IASTName>) astStack.pop();
    	 boolean hasDColon = astStack.pop() == PLACE_HOLDER;
    	 IASTName name = createQualifiedName(nestedNames, hasDColon);
    	 
     	 ICPPASTPointerToMember pointer = nodeFactory.newPointerToMember(name);
     	 addCVQualifiersToPointer(pointer, qualifiers);
     	 setOffsetAndLength(pointer);
		 astStack.push(pointer);
     	
     	 if(TRACE_AST_STACK) System.out.println(astStack);
     }

     
     
     /**
      * initializer
      *     ::= '(' expression_list ')'
      */
     public void consumeInitializerConstructor() {
    	 if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	 
    	 IASTExpression expression = (IASTExpression) astStack.pop();
    	 ICPPASTConstructorInitializer initializer = nodeFactory.newConstructorInitializer(expression);
    	 setOffsetAndLength(initializer);
		 astStack.push(initializer);
		 
    	 if(TRACE_AST_STACK) System.out.println(astStack);
     }
     
     
    /**
 	 * function_direct_declarator
     *     ::= basic_direct_declarator '(' <openscope-ast> parameter_declaration_clause ')' 
     *         <openscope-ast> cv_qualifier_seq_opt <openscope-ast> exception_specification_opt
 	 */
 	public void consumeDirectDeclaratorFunctionDeclarator(boolean hasDeclarator) {
 		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
 		
 		IASTName name = nodeFactory.newName();
 		ICPPASTFunctionDeclarator declarator = nodeFactory.newCPPFunctionDeclarator(name);
 		
 		for(Object typeId : astStack.closeScope()) {
 			declarator.addExceptionSpecificationTypeId((IASTTypeId) typeId);
 		}
 		
 		for(Object token : astStack.closeScope()) {
 			int kind = ((IToken)token).getKind();
 			switch(kind) {
 				default: assert false : "wrong token kind: " + kind; //$NON-NLS-1$
 				case TK_const:    declarator.setConst(true); break;
 				case TK_volatile: declarator.setVolatile(true); break;
 			}
 		}
 		
 		boolean isVarArgs = astStack.pop() == PLACE_HOLDER;
 		declarator.setVarArgs(isVarArgs);
 			
 		for(Object o : astStack.closeScope()) {
 			declarator.addParameterDeclaration((IASTParameterDeclaration)o);
 		}
 		
 		if(hasDeclarator) {
 			int endOffset = endOffset(parser.getRightIToken());
 			addFunctionModifier(declarator, endOffset);
 		}
 		else {
 			setOffsetAndLength(declarator);
			astStack.push(declarator);
 		}
 	}
 
 	
 	/**
 	 * mem_initializer
     *     ::= mem_initializer_id '(' expression_list_opt ')'
 	 */
 	public void consumeConstructorChainInitializer() {
 		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
 		
 		IASTExpression expr = (IASTExpression) astStack.pop();
 		IASTName name = (IASTName) astStack.pop();
 		ICPPASTConstructorChainInitializer initializer = nodeFactory.newConstructorChainInitializer(name, expr);
 		setOffsetAndLength(initializer);
		astStack.push(initializer);
 		
 		if(TRACE_AST_STACK) System.out.println(astStack);
 	}
 	
 	
 	
 	/**
 	 * function_definition
     *     ::= declaration_specifiers_opt function_direct_declarator 
     *         <openscope-ast> ctor_initializer_list_opt function_body
     *         
     *       | declaration_specifiers_opt function_direct_declarator 
     *         'try' <openscope-ast> ctor_initializer_list_opt function_body <openscope-ast> handler_seq
     *         
 	 */
 	public void consumeFunctionDefinition(boolean isTryBlockDeclarator) {
 		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();

 		List<Object> handlers = isTryBlockDeclarator ? astStack.closeScope() : Collections.emptyList();
 		IASTCompoundStatement body = (IASTCompoundStatement) astStack.pop();
 		List<Object> initializers = astStack.closeScope();
 		ICPPASTFunctionDeclarator declarator = (ICPPASTFunctionDeclarator) astStack.pop();
 		IASTDeclSpecifier declSpec = (IASTDeclSpecifier) astStack.pop(); // may be null
 		
 		if(declSpec == null) { // can happen if implicit int is used
 			declSpec = nodeFactory.newSimpleDeclSpecifier();
			setOffsetAndLength(declSpec, parser.getLeftIToken().getStartOffset(), 0);
		}
 		
 		if(isTryBlockDeclarator) {
 		    // perform a shallow copy 
 			ICPPASTFunctionTryBlockDeclarator tryBlockDeclarator = nodeFactory.newFunctionTryBlockDeclarator(declarator.getName());
 			tryBlockDeclarator.setConst(declarator.isConst());
 			tryBlockDeclarator.setVolatile(declarator.isVolatile());
 			tryBlockDeclarator.setPureVirtual(declarator.isPureVirtual());
 			tryBlockDeclarator.setVarArgs(declarator.takesVarArgs());
 			for(IASTParameterDeclaration parameter : declarator.getParameters()) {
 				tryBlockDeclarator.addParameterDeclaration(parameter);
 			}
 			for(IASTTypeId exception : declarator.getExceptionSpecification()) {
 				tryBlockDeclarator.addExceptionSpecificationTypeId(exception);
 			}
 			for(Object handler : handlers) {
 				tryBlockDeclarator.addCatchHandler((ICPPASTCatchHandler)handler);
 	 		}
 			
 			declarator = tryBlockDeclarator;
 		}
 		
 		
 		if(initializers != null && !initializers.isEmpty()) {
 			for(Object initializer : initializers)
 	 			declarator.addConstructorToChain((ICPPASTConstructorChainInitializer)initializer);
 			
 			// recalculate the length of the declarator to include the initializers
 			IASTNode lastInitializer = (IASTNode)initializers.get(initializers.size()-1);
 			int offset = offset(declarator);
 			int length = endOffset(lastInitializer) - offset;
 			setOffsetAndLength(declarator, offset, length);
 		}

 		IASTFunctionDefinition definition = nodeFactory.newFunctionDefinition(declSpec, declarator, body);
 		
 		setOffsetAndLength(definition);
		astStack.push(definition);
 			
 		if(TRACE_AST_STACK) System.out.println(astStack);
 	}
 	
 	
 	/**
 	 * member_declaration
 	 *     ::= dcolon_opt nested_name_specifier template_opt unqualified_id_name ';'
 	 */
 	public void consumeMemberDeclarationQualifiedId() {
 		if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
 		
 		IASTName qualifiedId = subRuleQualifiedName(true);
 		IASTDeclarator declarator = nodeFactory.newDeclarator(qualifiedId);
 		setOffsetAndLength(declarator);
 		// there has to be an empty specifier or... kaboom!
 		IASTDeclSpecifier emptySpecifier = nodeFactory.newSimpleDeclSpecifier();
		setOffsetAndLength(emptySpecifier, parser.getLeftIToken().getStartOffset(), 0);
 		IASTSimpleDeclaration declaration = nodeFactory.newSimpleDeclaration(emptySpecifier);
 		setOffsetAndLength(declaration);
 		declaration.addDeclarator(declarator);
 		astStack.push(declaration);
 		
 		if(TRACE_AST_STACK) System.out.println(astStack);
 	}
 	
 	
 	/**
 	 * member_declarator
     *     ::= declarator constant_initializer
 	 */
 	
    public void consumeMemberDeclaratorWithInitializer() {
    	if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	
    	IASTInitializerExpression initializer = (IASTInitializerExpression) astStack.pop();
    	IASTDeclarator declarator = (IASTDeclarator) astStack.peek();
    	setOffsetAndLength(declarator);
    	
    	if(declarator instanceof ICPPASTFunctionDeclarator) {
    		IASTExpression expr = initializer.getExpression();
    		if(expr instanceof IASTLiteralExpression && "0".equals(expr.toString())) { //$NON-NLS-1$
    			((ICPPASTFunctionDeclarator)declarator).setPureVirtual(true);
    			return;
    		}
    	}
    	
    	declarator.setInitializer(initializer);
    	
    	if(TRACE_AST_STACK) System.out.println(astStack);
    }

    
    /**
     * type_parameter
     *     ::= 'class' identifier_name_opt -- simple type template parameter     
     *       | 'class' identifier_name_opt '=' type_id
     *       | 'typename' identifier_name_opt
     *       | 'typename' identifier_name_opt '=' type_id
     */
    public void consumeSimpleTypeTemplateParameter(boolean hasTypeId) {
    	if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	
    	IASTTypeId typeId = hasTypeId ? (IASTTypeId)astStack.pop() : null;
    	
    	IASTName name = (IASTName)astStack.pop();
    	int type = getTemplateParameterType(parser.getLeftIToken()); 
    	
    	ICPPASTSimpleTypeTemplateParameter templateParameter = nodeFactory.newSimpleTypeTemplateParameter(type, name, typeId);
    	
    	setOffsetAndLength(templateParameter);
		astStack.push(templateParameter);
		
    	if(TRACE_AST_STACK) System.out.println(astStack);
    }
    
    private static int getTemplateParameterType(IToken token) {
    	int kind = token.getKind();
    	switch(kind) {
    		default: assert false : "wrong token kind: " + kind; //$NON-NLS-1$
    		case TK_class:    return ICPPASTSimpleTypeTemplateParameter.st_class;
    		case TK_typename: return ICPPASTSimpleTypeTemplateParameter.st_typename;
    	}
    }
    
    
    /**
     * Simple type template parameters using the 'class' keyword are being parsed
     * wrong due to an ambiguity between type_parameter and parameter_declaration.
     * 
     * eg) template <class T>
     * 
     * The 'class T' part is being parsed as an elaborated type specifier instead
     * of a simple type template parameter.
     * 
     * This method detects the incorrect parse, throws away the incorrect AST fragment,
     * and replaces it with the correct AST fragment.
     */
    public void consumeTemplateParamterDeclaration() {
    	if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	
    	if(matchTokens(parser.getRuleTokens(), TK_class, TK_identifier)) {
    		astStack.pop(); // throw away the ICPPASTParameterDeclaration
    		IASTName name = createName(parser.getRightIToken());
    		astStack.push(name);
    		consumeSimpleTypeTemplateParameter(false);
    	}
    	
    	if(TRACE_AST_STACK) System.out.println(astStack);
    }
    
    
    
    /**
     * type_parameter
     *     ::= 'template' '<' <openscope-ast> template_parameter_list '>' 'class' identifier_name_opt
     *       | 'template' '<' <openscope-ast> template_parameter_list '>' 'class' identifier_name_opt '=' id_expression
     * @param hasIdExpr
     */
    public void consumeTemplatedTypeTemplateParameter(boolean hasIdExpr) {
    	if(TRACE_ACTIONS) DebugUtil.printMethodTrace();
    	
    	IASTExpression idExpression = hasIdExpr ? (IASTExpression)astStack.pop() : null;
    	IASTName name = (IASTName) astStack.pop();
    	
    	ICPPASTTemplatedTypeTemplateParameter templateParameter = nodeFactory.newTemplatedTypeTemplateParameter(name, idExpression);
    	
    	for(Object param : astStack.closeScope())
    		templateParameter.addTemplateParamter((ICPPASTTemplateParameter)param);
    	
    	setOffsetAndLength(templateParameter);
		astStack.push(templateParameter);
    	
    	if(TRACE_AST_STACK) System.out.println(astStack);
    }
    
    
}




























