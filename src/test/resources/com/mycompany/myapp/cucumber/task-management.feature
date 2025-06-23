Feature: Daily Task Planner Management
  As a user
  I want to manage my daily tasks
  So that I can keep track of my daily activities and stay organized

  Background:
    Given I am logged in as a user

  Scenario: Add a new task with description only
    When I create a task with description "Complete project documentation"
    Then the task should be saved successfully
    And the task should appear in the task list
    And the task status should be incomplete
    And the task creation date should be set automatically

  Scenario: Add a new task with description and due date
    When I create a task with description "Review code changes" and due date "2024-02-15"
    Then the task should be saved successfully
    And the task should have due date "2024-02-15"
    And the task should appear in the task list

  Scenario: Add a new task with description, due date and priority
    When I create a task with description "Prepare presentation" and due date "2024-02-20" and priority "HIGH"
    Then the task should be saved successfully
    And the task should have priority "HIGH"
    And the task should appear in the task list

  Scenario: Fail to add task with empty description
    When I try to create a task with empty description
    Then the task creation should fail
    And I should see a validation error

  Scenario: Fail to add task with description longer than 255 characters
    When I try to create a task with description longer than 255 characters
    Then the task creation should fail
    And I should see a validation error

  Scenario: Edit an existing task description
    Given I have a task with description "Original task description"
    When I update the task description to "Updated task description"
    Then the task should be updated successfully
    And the task description should be "Updated task description"
    And the last modified date should be updated

  Scenario: Edit an existing task due date
    Given I have a task with description "Task with due date" and due date "2024-02-10"
    When I update the task due date to "2024-02-25"
    Then the task should be updated successfully
    And the task should have due date "2024-02-25"

  Scenario: Edit an existing task priority
    Given I have a task with description "Task with priority" and priority "LOW"
    When I update the task priority to "HIGH"
    Then the task should be updated successfully
    And the task should have priority "HIGH"

  Scenario: Delete an existing task
    Given I have a task with description "Task to be deleted"
    When I delete the task
    Then the task should be deleted successfully
    And the task should not appear in the task list

  Scenario: Delete a non-existing task
    When I try to delete a task with id 99999
    Then the task deletion should fail
    And I should see an error message

  Scenario: Mark a task as complete
    Given I have an incomplete task with description "Task to complete"
    When I mark the task as complete
    Then the task should be updated successfully
    And the task status should be complete

  Scenario: Mark a task as incomplete
    Given I have a complete task with description "Task to mark incomplete"
    When I mark the task as incomplete
    Then the task should be updated successfully
    And the task status should be incomplete

  Scenario: Sort tasks by due date ascending
    Given I have the following tasks:
      | description       | dueDate    | priority |
      | Task C           | 2024-02-20 | MEDIUM   |
      | Task A           | 2024-02-10 | HIGH     |
      | Task B           | 2024-02-15 | LOW      |
    When I sort tasks by due date in ascending order
    Then the tasks should be ordered by due date ascending

  Scenario: Sort tasks by due date descending
    Given I have the following tasks:
      | description       | dueDate    | priority |
      | Task C           | 2024-02-20 | MEDIUM   |
      | Task A           | 2024-02-10 | HIGH     |
      | Task B           | 2024-02-15 | LOW      |
    When I sort tasks by due date in descending order
    Then the tasks should be ordered by due date descending

  Scenario: Sort tasks by priority ascending
    Given I have the following tasks:
      | description       | dueDate    | priority |
      | Task A           | 2024-02-10 | HIGH     |
      | Task B           | 2024-02-15 | LOW      |
      | Task C           | 2024-02-20 | MEDIUM   |
    When I sort tasks by priority in ascending order
    Then the tasks should be ordered by priority ascending

  Scenario: Sort tasks by priority descending
    Given I have the following tasks:
      | description       | dueDate    | priority |
      | Task A           | 2024-02-10 | HIGH     |
      | Task B           | 2024-02-15 | LOW      |
      | Task C           | 2024-02-20 | MEDIUM   |
    When I sort tasks by priority in descending order
    Then the tasks should be ordered by priority descending

  Scenario: View tasks filtered by completion status
    Given I have the following tasks:
      | description       | completed |
      | Completed Task 1  | true      |
      | Incomplete Task 1 | false     |
      | Completed Task 2  | true      |
      | Incomplete Task 2 | false     |
    When I filter tasks by completion status "completed"
    Then I should see only completed tasks
    And I should see 2 tasks

  Scenario: View tasks filtered by incomplete status
    Given I have the following tasks:
      | description       | completed |
      | Completed Task 1  | true      |
      | Incomplete Task 1 | false     |
      | Completed Task 2  | true      |
      | Incomplete Task 2 | false     |
    When I filter tasks by completion status "incomplete"
    Then I should see only incomplete tasks
    And I should see 2 tasks

  Scenario: View all user's tasks
    Given I have 5 tasks created by me
    And another user has 3 tasks
    When I view my tasks
    Then I should see only my 5 tasks
    And I should not see other user's tasks 