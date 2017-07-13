/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package cofix.core.parser.node.stmt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import cofix.common.util.Pair;
import cofix.core.metric.CondStruct;
import cofix.core.metric.Literal;
import cofix.core.metric.LoopStruct;
import cofix.core.metric.MethodCall;
import cofix.core.metric.NewFVector;
import cofix.core.metric.Operator;
import cofix.core.metric.OtherStruct;
import cofix.core.metric.Variable;
import cofix.core.modify.Deletion;
import cofix.core.modify.Insertion;
import cofix.core.modify.Modification;
import cofix.core.modify.Revision;
import cofix.core.parser.NodeUtils;
import cofix.core.parser.node.Node;
import cofix.core.parser.node.expr.SName;

/**
 * @author Jiajun
 * @datae Jun 23, 2017
 */
public class Blk extends Stmt {

	private List<Stmt> _statements = null;
	
	private String _statements_replace = null;
	
	private int WHOLE = 10000;
	
	/**
	 * Block:
     *	{ { Statement } }
	 */
	public Blk(int startLine, int endLine, ASTNode node) {
		this(startLine, endLine, node, null);
		_nodeType = TYPE.BLOCK;
	}
	
	public Blk(int startLine, int endLine, ASTNode node, Node parent) {
		super(startLine, endLine, node, parent);
	}
	
	public void setStatement(List<Stmt> statements){
		_statements = statements;
	}

	@Override
	public boolean match(Node node, Map<String, String> varTrans, Map<String, Type> allUsableVariables, List<Modification> modifications) {
		boolean match = false;
		if(node instanceof Blk){
			match = true;
			Blk other = (Blk) node;
			if(_statements.size() == 1 && other._statements.size() == 1){
				Node thisNode = _statements.get(0);
				Node otherNode = other._statements.get(0);
				if(otherNode instanceof ThrowStmt || otherNode instanceof ReturnStmt){
					String source = thisNode.toSrcString().toString(); 
					if(!source.equals(otherNode.toSrcString().toString())){
						Map<SName, Pair<String, String>> record = NodeUtils.tryReplaceAllVariables(otherNode, varTrans, allUsableVariables);
						if(record != null){
							NodeUtils.replaceVariable(record);
							String target = otherNode.toSrcString().toString();
							if(!source.equals(target)){
								Revision revision = new Revision(this, WHOLE, target, _nodeType);
								modifications.add(revision);
							}
							NodeUtils.restoreVariables(record);
						}
					}
				}
			} else {
				modifications.addAll(NodeUtils.listNodeMatching(this, _nodeType, _statements, other._statements, varTrans, allUsableVariables));
			}
		}
		return match;
	}

	@Override
	public boolean adapt(Modification modification) {
		int index = modification.getSourceID();
		if(index == WHOLE){
			_statements_replace = modification.getTargetString();
		} else if(index < _statements.size()){
			if(modification instanceof Deletion){
				StringBuffer stringBuffer = new StringBuffer();
				for(int i = 0; i < _statements.size(); i++){
					if(i == index){
						continue;
					}
					stringBuffer.append(_statements.get(i).toSrcString());
					stringBuffer.append("\n");
				}
				_statements_replace = stringBuffer.toString();
			} else if(modification instanceof Insertion){
				StringBuffer stringBuffer = new StringBuffer();
				for(int i = 0; i < _statements.size(); i++){
					if(i == index){
						stringBuffer.append(modification.getTargetString());
						stringBuffer.append("\n");
					}
					stringBuffer.append(_statements.get(i).toSrcString());
					stringBuffer.append("\n");
				}
				_statements_replace = stringBuffer.toString();
			}
		} else {
			return false;
		}
		return true;
	}

	@Override
	public boolean restore(Modification modification) {
		_statements_replace = null;
		return false;
	}

	@Override
	public boolean backup(Modification modification) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("{\n");
		if(_statements_replace != null){
			stringBuffer.append(_statements_replace);
			stringBuffer.append("\n");
		} else {
			for(Stmt stmt : _statements){
				stringBuffer.append(stmt.toSrcString());
				stringBuffer.append("\n");
			}
		}
		stringBuffer.append("}");
		return stringBuffer;
	}

	@Override
	public List<Literal> getLiterals() {
		List<Literal> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getLiterals());
			}
		}
		return list;
	}

	@Override
	public List<Variable> getVariables() {
		List<Variable> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getVariables());
			}
		}
		return list;
	}

	@Override
	public List<LoopStruct> getLoopStruct() {
		List<LoopStruct> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getLoopStruct());
			}
		}
		return list;
	}
	
	@Override
	public List<CondStruct> getCondStruct() {
		List<CondStruct> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getCondStruct());
			}
		}
		return list;
	}

	@Override
	public List<MethodCall> getMethodCalls() {
		List<MethodCall> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getMethodCalls());
			}
		}
		return list;
	}

	@Override
	public List<Operator> getOperators() {
		List<Operator> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getOperators());
			}
		}
		return list;
	}
	
	@Override
	public List<OtherStruct> getOtherStruct() {
		List<OtherStruct> list = new LinkedList<>();;
		if(_statements != null){
			for(Stmt stmt : _statements){
				list.addAll(stmt.getOtherStruct());
			}
		}
		return list;
	}
	
	@Override
	public void computeFeatureVector() {
		_fVector = new NewFVector();
		if(_statements != null){
			for(Stmt stmt : _statements){
				_fVector.combineFeature(stmt.getFeatureVector());
			}
		}
	}
	
	@Override
	public List<Node> getChildren() {
		List<Node> list = new ArrayList<>();
		for(Stmt stmt : _statements){
			list.add(stmt);
		}
		return list;
	}
	
	@Override
	public String simplify(Map<String, String> varTrans, Map<String, Type> allUsableVariables) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("{\n");
		boolean empty = true;
		for(Stmt stmt : _statements){
			String string = stmt.simplify(varTrans, allUsableVariables);
			if(string != null){
				empty = false;
				stringBuffer.append(string);
				stringBuffer.append("\n");
			}
		}
		stringBuffer.append("}");
		if(empty){
			return null;
		}
		return stringBuffer.toString();
	}
	
}