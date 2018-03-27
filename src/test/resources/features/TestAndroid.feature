# language: en

Feature: Example

  @dev
  Scenario: TestInstall&Play
    Given open Play Market
    When input app name "Scatter Slots Murka"
    When select app "Игровые Автоматы Scatter Slots"
    Then check app version "3.21.0"
    When install app
    Then start app
    When select position in buy menu

