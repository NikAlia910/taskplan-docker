import './task.scss';

import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  Col,
  Row,
  Input,
  Form,
  FormGroup,
  Label,
  Badge,
  ButtonGroup,
  InputGroup,
  UncontrolledTooltip,
} from 'reactstrap';
import { JhiItemCount, JhiPagination, TextFormat, Translate, getPaginationState } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faPlus,
  faCheck,
  faTimes,
  faEdit,
  faTrash,
  faCalendarAlt,
  faFlag,
  faFilter,
  faSort,
  faSortUp,
  faSortDown,
  faCheckCircle,
  faCircle,
  faRefresh,
} from '@fortawesome/free-solid-svg-icons';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import dayjs from 'dayjs';

import { getEntities, createEntity, toggleTaskCompletion } from './task.reducer';
import { ITask } from 'app/shared/model/task.model';
import { TaskPriority } from 'app/shared/model/enumerations/task-priority.model';

export const Task = () => {
  const dispatch = useAppDispatch();
  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [paginationState, setPaginationState] = useState(
    overridePaginationStateWithQueryParams(getPaginationState(pageLocation, ITEMS_PER_PAGE, 'createdDate'), pageLocation.search),
  );

  // Task planner specific state
  const [newTaskDescription, setNewTaskDescription] = useState('');
  const [newTaskDueDate, setNewTaskDueDate] = useState('');
  const [newTaskPriority, setNewTaskPriority] = useState<TaskPriority>(TaskPriority.MEDIUM);
  const [filterCompleted, setFilterCompleted] = useState<'all' | 'completed' | 'incomplete'>('all');
  const [sortBy, setSortBy] = useState<'dueDate' | 'priority' | 'createdDate'>('createdDate');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  const taskList = useAppSelector(state => state.task.entities);
  const loading = useAppSelector(state => state.task.loading);
  const updating = useAppSelector(state => state.task.updating);
  const totalItems = useAppSelector(state => state.task.totalItems);

  const getAllEntities = () => {
    const completed = filterCompleted === 'all' ? undefined : filterCompleted === 'completed';
    dispatch(
      getEntities({
        page: paginationState.activePage - 1,
        size: paginationState.itemsPerPage,
        sort: `${sortBy},${sortOrder}`,
        completed,
        currentUserOnly: true,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const params = new URLSearchParams(pageLocation.search);
    params.set('page', paginationState.activePage.toString());
    params.set('sort', `${sortBy},${sortOrder}`);
    if (filterCompleted !== 'all') {
      params.set('filter', filterCompleted);
    }
    navigate(`${pageLocation.pathname}?${params.toString()}`);
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, sortBy, sortOrder, filterCompleted]);

  useEffect(() => {
    const params = new URLSearchParams(pageLocation.search);
    const page = params.get('page');
    const sort = params.get('sort');
    const filter = params.get('filter');

    if (page) {
      setPaginationState(prev => ({
        ...prev,
        activePage: +page,
      }));
    }

    if (sort) {
      const [field, order] = sort.split(',');
      setSortBy(field as 'dueDate' | 'priority' | 'createdDate');
      setSortOrder(order as 'asc' | 'desc');
    }

    if (filter) {
      setFilterCompleted(filter as 'all' | 'completed' | 'incomplete');
    }
  }, [pageLocation.search]);

  const handlePagination = currentPage =>
    setPaginationState({
      ...paginationState,
      activePage: currentPage,
    });

  const handleSyncList = () => {
    getAllEntities();
  };

  const handleQuickTaskCreate = e => {
    e.preventDefault();
    if (newTaskDescription.trim()) {
      const newTask: ITask = {
        description: newTaskDescription.trim(),
        dueDate: newTaskDueDate ? dayjs(newTaskDueDate) : null,
        priority: newTaskPriority,
        completed: false,
      };

      dispatch(createEntity(newTask)).then(() => {
        setNewTaskDescription('');
        setNewTaskDueDate('');
        setNewTaskPriority(TaskPriority.MEDIUM);
        getAllEntities();
      });
    }
  };

  const handleToggleCompletion = (taskId: number) => {
    dispatch(toggleTaskCompletion(taskId)).then(() => {
      getAllEntities();
    });
  };

  const handleSort = (field: 'dueDate' | 'priority' | 'createdDate') => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortOrder('asc');
    }
  };

  const getPriorityBadgeColor = (priority: TaskPriority) => {
    switch (priority) {
      case TaskPriority.HIGH:
        return 'danger';
      case TaskPriority.MEDIUM:
        return 'warning';
      case TaskPriority.LOW:
        return 'success';
      default:
        return 'secondary';
    }
  };

  const getSortIcon = (field: string) => {
    if (sortBy !== field) return faSort;
    return sortOrder === 'asc' ? faSortUp : faSortDown;
  };

  const getCompletedTasksCount = () => taskList.filter(task => task.completed).length;
  const getTotalTasksCount = () => taskList.length;

  return (
    <div className="container-fluid daily-task-planner">
      <Row>
        <Col lg="12">
          <div className="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h2 className="text-primary mb-1" data-cy="TaskHeading">
                <FontAwesomeIcon icon={faCalendarAlt} className="me-2" />
                Daily Task Planner
              </h2>
              <small className="text-muted">
                {getCompletedTasksCount()} of {getTotalTasksCount()} tasks completed
              </small>
            </div>
            <div>
              <Button color="outline-primary" size="sm" onClick={handleSyncList} disabled={loading} className="me-2">
                <FontAwesomeIcon icon={faRefresh} spin={loading} />
              </Button>
              <Link to="/task/new" className="btn btn-primary btn-sm" data-cy="entityCreateButton">
                <FontAwesomeIcon icon={faPlus} /> Advanced Add
              </Link>
            </div>
          </div>
        </Col>
      </Row>

      {/* Quick Task Creation */}
      <Row className="mb-4">
        <Col lg="12">
          <Card>
            <CardHeader>
              <FontAwesomeIcon icon={faPlus} className="me-2" />
              Quick Add Task
            </CardHeader>
            <CardBody>
              <Form onSubmit={handleQuickTaskCreate} className="quick-add-form">
                <Row>
                  <Col md="6">
                    <FormGroup>
                      <Input
                        type="text"
                        placeholder="What needs to be done?"
                        value={newTaskDescription}
                        onChange={e => setNewTaskDescription(e.target.value)}
                        maxLength={255}
                        required
                      />
                    </FormGroup>
                  </Col>
                  <Col md="2">
                    <FormGroup>
                      <Input type="date" value={newTaskDueDate} onChange={e => setNewTaskDueDate(e.target.value)} title="Due Date" />
                    </FormGroup>
                  </Col>
                  <Col md="2">
                    <FormGroup>
                      <Input
                        type="select"
                        value={newTaskPriority}
                        onChange={e => setNewTaskPriority(e.target.value as TaskPriority)}
                        title="Priority"
                      >
                        <option value={TaskPriority.LOW}>Low</option>
                        <option value={TaskPriority.MEDIUM}>Medium</option>
                        <option value={TaskPriority.HIGH}>High</option>
                      </Input>
                    </FormGroup>
                  </Col>
                  <Col md="2">
                    <Button type="submit" color="success" block disabled={!newTaskDescription.trim() || updating}>
                      <FontAwesomeIcon icon={faPlus} />
                    </Button>
                  </Col>
                </Row>
              </Form>
            </CardBody>
          </Card>
        </Col>
      </Row>

      {/* Filters and Sorting */}
      <Row className="mb-3">
        <Col md="6">
          <div className="d-flex align-items-center">
            <FontAwesomeIcon icon={faFilter} className="me-2 text-muted" />
            <ButtonGroup className="filter-buttons">
              <Button size="sm" color={filterCompleted === 'all' ? 'primary' : 'outline-primary'} onClick={() => setFilterCompleted('all')}>
                All ({getTotalTasksCount()})
              </Button>
              <Button
                size="sm"
                color={filterCompleted === 'incomplete' ? 'primary' : 'outline-primary'}
                onClick={() => setFilterCompleted('incomplete')}
              >
                Active ({getTotalTasksCount() - getCompletedTasksCount()})
              </Button>
              <Button
                size="sm"
                color={filterCompleted === 'completed' ? 'primary' : 'outline-primary'}
                onClick={() => setFilterCompleted('completed')}
              >
                Completed ({getCompletedTasksCount()})
              </Button>
            </ButtonGroup>
          </div>
        </Col>
        <Col md="6">
          <div className="d-flex justify-content-end align-items-center">
            <span className="me-2 text-muted">Sort by:</span>
            <ButtonGroup size="sm" className="sort-buttons">
              <Button color={sortBy === 'createdDate' ? 'primary' : 'outline-primary'} onClick={() => handleSort('createdDate')}>
                Created <FontAwesomeIcon icon={getSortIcon('createdDate')} />
              </Button>
              <Button color={sortBy === 'dueDate' ? 'primary' : 'outline-primary'} onClick={() => handleSort('dueDate')}>
                Due Date <FontAwesomeIcon icon={getSortIcon('dueDate')} />
              </Button>
              <Button color={sortBy === 'priority' ? 'primary' : 'outline-primary'} onClick={() => handleSort('priority')}>
                Priority <FontAwesomeIcon icon={getSortIcon('priority')} />
              </Button>
            </ButtonGroup>
          </div>
        </Col>
      </Row>

      {/* Task List */}
      <Row>
        <Col lg="12">
          <div className="task-list" data-cy="entityTable">
            {taskList && taskList.length > 0
              ? taskList.map((task, i) => (
                  <Card key={`task-${i}`} className={`mb-3 ${task.completed ? 'bg-light' : ''}`}>
                    <CardBody>
                      <Row className="align-items-center">
                        <Col xs="auto">
                          <Button
                            color="link"
                            className="p-0 border-0 task-completion-toggle"
                            onClick={() => handleToggleCompletion(task.id)}
                            disabled={updating}
                            id={`toggle-${task.id}`}
                          >
                            <FontAwesomeIcon
                              icon={task.completed ? faCheckCircle : faCircle}
                              className={task.completed ? 'text-success' : 'text-secondary'}
                              size="lg"
                            />
                          </Button>
                          <UncontrolledTooltip target={`toggle-${task.id}`}>
                            {task.completed ? 'Mark as incomplete' : 'Mark as complete'}
                          </UncontrolledTooltip>
                        </Col>
                        <Col>
                          <div className={task.completed ? 'text-decoration-line-through text-muted' : ''}>
                            <h6 className="mb-1">{task.description}</h6>
                            <div className="d-flex align-items-center">
                              {task.dueDate && (
                                <Badge color="info" className="me-2">
                                  <FontAwesomeIcon icon={faCalendarAlt} className="me-1" />
                                  <TextFormat type="date" value={task.dueDate} format={APP_LOCAL_DATE_FORMAT} />
                                </Badge>
                              )}
                              {task.priority && (
                                <Badge color={getPriorityBadgeColor(task.priority)} className="me-2">
                                  <FontAwesomeIcon icon={faFlag} className="me-1" />
                                  <Translate contentKey={`taskplanDockerApp.TaskPriority.${task.priority}`} />
                                </Badge>
                              )}
                              <small className="text-muted">
                                Created: <TextFormat type="date" value={task.createdDate} format={APP_LOCAL_DATE_FORMAT} />
                              </small>
                            </div>
                          </div>
                        </Col>
                        <Col xs="auto">
                          <div className="btn-group">
                            <Button
                              tag={Link}
                              to={`/task/${task.id}`}
                              color="outline-info"
                              size="sm"
                              title="View Details"
                              data-cy="entityDetailsButton"
                            >
                              <FontAwesomeIcon icon="eye" />
                            </Button>
                            <Button
                              tag={Link}
                              to={`/task/${task.id}/edit`}
                              color="outline-primary"
                              size="sm"
                              title="Edit Task"
                              data-cy="entityEditButton"
                            >
                              <FontAwesomeIcon icon={faEdit} />
                            </Button>
                            <Button
                              onClick={() => (window.location.href = `/task/${task.id}/delete`)}
                              color="outline-danger"
                              size="sm"
                              title="Delete Task"
                              data-cy="entityDeleteButton"
                            >
                              <FontAwesomeIcon icon={faTrash} />
                            </Button>
                          </div>
                        </Col>
                      </Row>
                    </CardBody>
                  </Card>
                ))
              : !loading && (
                  <Card>
                    <CardBody className="text-center py-5 empty-state">
                      <FontAwesomeIcon icon={faCalendarAlt} size="3x" className="text-muted mb-3" />
                      <h4 className="text-muted">
                        {filterCompleted === 'all'
                          ? 'No tasks yet'
                          : filterCompleted === 'completed'
                            ? 'No completed tasks'
                            : 'No active tasks'}
                      </h4>
                      <p className="text-muted">
                        {filterCompleted === 'all'
                          ? 'Create your first task to get started!'
                          : filterCompleted === 'completed'
                            ? 'Complete some tasks to see them here.'
                            : 'All tasks are completed!'}
                      </p>
                      {filterCompleted === 'all' && (
                        <Link to="/task/new" className="btn btn-primary">
                          <FontAwesomeIcon icon={faPlus} className="me-2" />
                          Create Your First Task
                        </Link>
                      )}
                    </CardBody>
                  </Card>
                )}
          </div>
        </Col>
      </Row>

      {/* Pagination */}
      {totalItems ? (
        <Row className="mt-4">
          <Col lg="12">
            <div className="d-flex justify-content-between align-items-center">
              <JhiItemCount page={paginationState.activePage} total={totalItems} itemsPerPage={paginationState.itemsPerPage} i18nEnabled />
              <JhiPagination
                activePage={paginationState.activePage}
                onSelect={handlePagination}
                maxButtons={5}
                itemsPerPage={paginationState.itemsPerPage}
                totalItems={totalItems}
              />
            </div>
          </Col>
        </Row>
      ) : null}
    </div>
  );
};

export default Task;
