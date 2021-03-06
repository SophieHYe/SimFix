#!/usr/bin/python2.7

import collections
import itertools
import csv
import os
import subprocess

import pyfl

Fault = collections.namedtuple('Fault', 'project bug_id')

def exists(*path_components):
  return os.path.exists(os.path.join(*path_components))
def print_macro(name, value):
  print(r'\def\{}{{{}\xspace}}'.format(name, value))
def print_counter(name, value):
  print(r'\newcounter{{{}}}'.format(name))
  print(r'\setcounter{{{}}}{{{}}}'.format(name, value))

os.chdir(os.path.join(os.environ['D4J_HOME'], 'framework', 'projects'))

PROJECTS = ['Math','Chart','Time','Lang','Closure','Mockito']
N_REAL_FAULTS_BY_PROJECT = {'Math':106, 'Closure':133, 'Time':27, 'Chart':26, 'Lang':65, 'Mockito':38}
def real_faults_for_artificial_faults(artificial_faults):
  result = set()
  for project in PROJECTS:
    with open(os.path.join(project, 'mutants_in_scope.csv')) as f:
      result |= set(Fault(project, int(b)) for _,b,af in csv.reader(f)
                    if Fault(project,int(af)) in artificial_faults)
  return result

print("%% These macros were automatically generated by {!r}.".format(__file__))

artificial_faults = set(Fault(project, int(af)) for project in PROJECTS for _,rf,af in csv.reader(open(os.path.join(project, 'mutants_in_scope.csv'))))
real_faults = set(Fault(project, bug) for project in PROJECTS for bug in range(1, N_REAL_FAULTS_BY_PROJECT[project]+1))

n_mutants_in_fixed_statements = sum(
  len(list(pyfl.iter_mutants_in_scope(
    mutants=pyfl.formats.iter_mutants_log_lines(open(os.path.join(os.environ['D4J_HOME'], 'framework', 'projects', project, 'trigger_tests', str(bug_id)+'.mutants.log'))),
    stmt_spans_file=os.path.join(os.environ['FL_DATA_HOME'], 'analysis', 'pipeline-scripts', 'source-code-lines', project+'-'+str(bug_id)+'f.source-code.lines'),
    fixed_lines_file=os.path.join(os.environ['FL_DATA_HOME'], 'analysis', 'pipeline-scripts', 'fixed-lines', project+'-'+str(bug_id)+'.fixed.lines')
  )))
  for project, n_faults in N_REAL_FAULTS_BY_PROJECT.items()
  for bug_id in range(1, n_faults+1))
print_macro('nMutantsInFixedStatements', n_mutants_in_fixed_statements)

pure_deletion_real_faults = set()
for fault in set(real_faults):
  with open(os.path.join(fault.project, 'patches', '{}.src.patch'.format(fault.bug_id))) as f:
    if len([line for line in f if line.startswith('-')]) < 2:
      pure_deletion_real_faults.add(fault)
real_faults -= pure_deletion_real_faults
print_macro('nRealFaultsThatAreNotPureDeletions', len(real_faults))


n_real_faults_before_checking_artificial_fault_validity = len(real_faults)

invalid_artificial_faults_by_reason = collections.defaultdict(set)
with open(os.path.join(os.environ['FL_DATA_HOME'], 'data', 'blacklist.csv')) as f:
  for row in csv.DictReader(f):
    invalid_artificial_faults_by_reason[row['reason']].add(Fault(row['project'], int(row['mutant_id'])))

artificial_faults -= invalid_artificial_faults_by_reason['checkout/compilation failed']
real_faults &= real_faults_for_artificial_faults(artificial_faults)
print_macro('nCompilableMutantsInFixedStatements', len(artificial_faults))
print_macro('nRealFaultsWithCompilableMutantsInFixedStatements', len(real_faults))

artificial_faults -= invalid_artificial_faults_by_reason['timeout']
real_faults &= real_faults_for_artificial_faults(artificial_faults)
print_macro('nCompilableMutantsInFixedStatementsWhoseTestsDoNotTimeOut', len(artificial_faults))
print_macro('nRealFaultsWithCompilableMutantsInFixedStatementsWhoseTestsDoNotTimeOut', len(real_faults))

artificial_faults -= invalid_artificial_faults_by_reason['failing test classes']
real_faults &= real_faults_for_artificial_faults(artificial_faults)
print_macro('nCompilableMutantsInFixedStatementsWhoseTestsCanAllBeRunAndDoNotTimeOut', len(artificial_faults))
print_macro('nRealFaultsWithCompilableMutantsInFixedStatementsWhoseTestsCanAllBeRunAndDoNotTimeOut', len(real_faults))

artificial_faults -= invalid_artificial_faults_by_reason['0 triggering test cases']
real_faults &= real_faults_for_artificial_faults(artificial_faults)
print_macro('nDetectableCompilableMutantsInFixedStatementsWhoseTestsCanAllBeRunAndDoNotTimeOut', len(artificial_faults))
print_counter('nDetectableCompilableMutantsInFixedStatementsWhoseTestsCanAllBeRunAndDoNotTimeOut', len(artificial_faults))
print_macro('nRealFaultsWithDetectableCompilableMutantsInFixedStatementsWhoseTestsCanAllBeRunAndDoNotTimeOut', len(real_faults))

print_macro('nRealFaultsWithSomeArtificialFaultsButOnlyInvalidOnes', n_real_faults_before_checking_artificial_fault_validity - len(real_faults))
print_counter('nRealFaultsWithSomeArtificialFaultsButOnlyInvalidOnes', n_real_faults_before_checking_artificial_fault_validity - len(real_faults))
