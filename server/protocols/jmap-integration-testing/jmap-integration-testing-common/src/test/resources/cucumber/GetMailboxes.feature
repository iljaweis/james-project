#***************************************************************
# Licensed to the Apache Software Foundation (ASF) under one   *
# or more contributor license agreements.  See the NOTICE file *
# distributed with this work for additional information        *
# regarding copyright ownership.  The ASF licenses this file   *
# to you under the Apache License, Version 2.0 (the            *
# "License"); you may not use this file except in compliance   *
# with the License.  You may obtain a copy of the License at   *
#                                                              *
#   http://www.apache.org/licenses/LICENSE-2.0                 *
#                                                              *
# Unless required by applicable law or agreed to in writing,   *
# software distributed under the License is distributed on an  *
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
# KIND, either express or implied.  See the License for the    *
# specific language governing permissions and limitations      *
# under the License.                                           *
# **************************************************************/
Feature: GetMailboxes method
  As a James user
  I want to be able to retrieve my mailboxes

  Background:
    Given a domain named "domain.tld"
    And a user "alice@domain.tld"
    And a user "bob@domain.tld"
    And "alice@domain.tld" has a mailbox "INBOX"
    And "bob@domain.tld" has a mailbox "bobMailbox"
    And "alice@domain.tld" has a mailbox "shared"
    And "alice@domain.tld" shares her mailbox "shared" with "bob@domain.tld" with "aeilrwt" rights

  Scenario: Sharer can read the total and unread counts on a shared folder
    Given "alice@domain.tld" has a message "m1" in "shared" mailbox
    And "alice@domain.tld" has a message "m2" in "shared" mailbox with subject "my test subject 2", content "testmail 2"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 2 messages
    And the mailbox "shared" has 2 unseen messages

  Scenario: Sharee can read the total and unread counts on a shared folder
    Given "alice@domain.tld" has a message "m1" in "shared" mailbox
    And "alice@domain.tld" has a message "m2" in "shared" mailbox with subject "my test subject 2", content "testmail 2"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 2 messages
    And the mailbox "shared" has 2 unseen messages

  Scenario: Copy message should update the total and the unread counts when asked by sharer
    Given "alice@domain.tld" has a message "m1" in "INBOX" mailbox
    And "alice@domain.tld" copies "m1" from mailbox "INBOX" to mailbox "shared"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Copy message should update the total and the unread counts when asked by sharer / sharee view
    Given "alice@domain.tld" has a message "m1" in "INBOX" mailbox
    And "alice@domain.tld" copies "m1" from mailbox "INBOX" to mailbox "shared"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Copy message should update the total and the unread counts when asked by sharee
    Given "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" copies "m1" from mailbox "bobMailbox" of user "bob@domain.tld" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Copy message should update the total and the unread counts when asked by sharee / sharee view
    Given "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" copies "m1" from mailbox "bobMailbox" of user "bob@domain.tld" to mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Move message should update the total and the unread counts when asked by sharer
    Given "alice@domain.tld" has a message "m1" in "INBOX" mailbox
    And "alice@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Move message should update the total and the unread counts of origin mailbox when asked by sharer
    Given "alice@domain.tld" has a message "m1" in "INBOX" mailbox
    And "alice@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "INBOX" has 0 messages
    And the mailbox "INBOX" has 0 unseen messages

  Scenario: Move message should update the total and the unread counts when asked by sharer / sharee view
    Given "alice@domain.tld" has a message "m1" in "INBOX" mailbox
    And "alice@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Move message should update the total and the unread counts of origin mailbox when asked by sharer / sharee view
    Given "alice@domain.tld" has a mailbox "sharedBis"
    And "alice@domain.tld" has a message "m1" in "sharedBis" mailbox
    And "alice@domain.tld" shares her mailbox "sharedBis" with "bob@domain.tld" with "aeilrwt" rights
    And "alice@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "sharedBis" has 0 messages
    And the mailbox "sharedBis" has 0 unseen messages

  Scenario: Move message should update the total and the unread counts when asked by sharee
    Given "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Move message should update the total and the unread counts of origin mailbox when asked by sharee
    Given "bob@domain.tld" has a mailbox "sharedBis"
    And "bob@domain.tld" has a message "m1" in "sharedBis" mailbox
    And "bob@domain.tld" shares her mailbox "sharedBis" with "alice@domain.tld" with "aeilrwt" rights
    And "bob@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "sharedBis" has 0 messages
    And the mailbox "sharedBis" has 0 unseen messages

  Scenario: Move message should update the total and the unread counts when asked by sharee / sharee view
    Given "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 1 unseen message

  Scenario: Move message should update the total and the unread counts of origin mailbox when asked by sharee / sharee view
    Given "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "bobMailbox" has 0 messages
    And the mailbox "bobMailbox" has 0 unseen messages

  Scenario: Moving a message to a delegated mailbox without rights should not change the total and the unread counts
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "lr" rights
    And "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" moves "m1" to mailbox "shared2" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 0 messages
    And the mailbox "shared" has 0 unseen messages

  Scenario: Moving a message to a delegated mailbox without rights should not change the total and the unread counts / sharee view
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "lr" rights
    And "bob@domain.tld" has a message "m1" in "bobMailbox" mailbox
    And "bob@domain.tld" moves "m1" to mailbox "shared2" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 0 messages
    And the mailbox "shared" has 0 unseen messages

  Scenario: Move message should update the total and the unread counts when asked by sharee and seen message
    Given "bob@domain.tld" has a message "m1" in the "bobMailbox" mailbox with flags "$Seen"
    And "bob@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 0 unseen message

  Scenario: Move message should update the total and the unread counts when asked by sharee / sharee view and seen message
    Given "bob@domain.tld" has a message "m1" in the "bobMailbox" mailbox with flags "$Seen"
    And "bob@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 0 unseen message

  Scenario: Move message should update the total and the unread counts of origin mailbox when asked by sharer and seen message
    Given "alice@domain.tld" has a message "m1" in the "INBOX" mailbox with flags "$Seen"
    And "alice@domain.tld" moves "m1" to mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "INBOX" has 0 messages
    And the mailbox "INBOX" has 0 unseen messages

  Scenario: Moving a message to a delegated mailbox without rights should not change the total and the unread counts
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "lri" rights
    And "bob@domain.tld" has a message "m1" in the "bobMailbox" mailbox with flags "$Seen"
    And "bob@domain.tld" moves "m1" to mailbox "shared2" of user "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 0 messages
    And the mailbox "shared" has 0 unseen messages

  Scenario: Moving a message to a delegated mailbox without rights should not change the total and the unread counts / sharee view
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "lri" rights
    And "bob@domain.tld" has a message "m1" in the "bobMailbox" mailbox with flags "$Seen"
    And "bob@domain.tld" moves "m1" to mailbox "shared2" of user "alice@domain.tld"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 0 messages
    And the mailbox "shared" has 0 unseen messages

  Scenario: Set flags by sharer should update unseen count when read by sharer
    Given "alice@domain.tld" has a message "m1" in "shared" mailbox
    When "alice@domain.tld" sets flags "$Seen" on message "m1"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 0 unseen messages

  Scenario: Set flags by sharer should update unseen count when read by sharee
    Given "alice@domain.tld" has a message "m1" in "shared" mailbox
    When "alice@domain.tld" sets flags "$Seen" on message "m1"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 0 unseen messages

  Scenario: Set flags by sharee should update unseen count when read by sharer
    Given "alice@domain.tld" has a message "m1" in "shared" mailbox
    When "bob@domain.tld" sets flags "$Seen" on message "m1"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 0 unseen messages

  Scenario: Set flags by sharee should update unseen count when read by sharee
    Given "alice@domain.tld" has a message "m1" in "shared" mailbox
    When "bob@domain.tld" sets flags "$Seen" on message "m1"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared" has 1 message
    And the mailbox "shared" has 0 unseen messages

  Scenario: Set flags by sharee should not update unseen count when no rights and read by sharer
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "lri" rights
    And "alice@domain.tld" has a message "m1" in "shared2" mailbox
    When "bob@domain.tld" sets flags "$Seen" on message "m1"
    When "alice@domain.tld" lists mailboxes
    Then the mailbox "shared2" has 1 message
    And the mailbox "shared2" has 1 unseen message

  Scenario: As sharee read a message it should not update unseen count when no rights and read by sharee
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "lri" rights
    And "alice@domain.tld" has a message "m1" in "shared2" mailbox
    When "bob@domain.tld" sets flags "$Seen" on message "m1"
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared2" has 1 message
    And the mailbox "shared2" has 1 unseen message

  Scenario: Lookup right should not be enough to read message and unseen counts
    Given "alice@domain.tld" has a mailbox "shared2"
    And "alice@domain.tld" shares her mailbox "shared2" with "bob@domain.tld" with "l" rights
    And "alice@domain.tld" has a message "m1" in "shared2" mailbox
    When "bob@domain.tld" lists mailboxes
    Then the mailbox "shared2" has 0 messages
    And the mailbox "shared2" has 0 unseen messages

  Scenario: User can share sub-mailbox without sharing its parent
    Given "alice@domain.tld" has a mailbox "mailbox1"
    And "alice@domain.tld" has a mailbox "mailbox1.shared"
    And "alice@domain.tld" shares her mailbox "mailbox1.shared" with "bob@domain.tld" with "aeirwt" rights
    When "bob@domain.tld" lists mailboxes
    Then the mailboxes should contain "shared" in "Delegated" namespace
    And the mailboxes should not contain "mailbox1"

  Scenario: User can share sub-mailbox without sharing its parent and then sharee can see the parent mailbox
    Given "alice@domain.tld" has a mailbox "mailbox1"
    And "alice@domain.tld" has a mailbox "mailbox1.shared"
    And "alice@domain.tld" shares her mailbox "mailbox1.shared" with "bob@domain.tld" with "l" rights
    When "bob@domain.tld" lists mailboxes
    Then the mailboxes should contain "shared" in "Delegated" namespace
    And the mailboxes should contain "mailbox1" in "Delegated" namespace

  Scenario: A sharee should be able to access a shared mailbox after it has been renamed by the owner
    Given "alice@domain.tld" renames her mailbox "shared" to "mySharedMailbox"
    When "bob@domain.tld" lists mailboxes
    Then the mailboxes should contain "mySharedMailbox" in "Delegated" namespace

  Scenario: A sharee should not be able to rename a shared mailbox
    Given "bob@domain.tld" renames the mailbox, owned by "alice@domain.tld", "shared" to "mySharedMailbox"
    When "alice@domain.tld" lists mailboxes
    Then the mailboxes should contain "shared" in "Personal" namespace

  Scenario: A user should not be able to rename an other user mailbox
    Given "alice@domain.tld" has a mailbox "mySecrets"
    And "bob@domain.tld" renames the mailbox, owned by "alice@domain.tld", "mySecrets" to "revealMySecrets"
    When "alice@domain.tld" lists mailboxes
    Then the mailboxes should contain "mySecrets" in "Personal" namespace

  Scenario: A sharee should receive a not updated response when trying to rename a shared mailbox
    Given "bob@domain.tld" renames the mailbox, owned by "alice@domain.tld", "shared" to "mySharedMailbox"
    Then mailbox "shared" owned by "alice@domain.tld" is not updated

  Scenario: A sharee should receive a not destroyed response when trying to destroy a shared mailbox
    Given "bob@domain.tld" deletes the mailbox "shared" owned by "alice@domain.tld"
    Then mailbox "shared" owned by "alice@domain.tld" is not destroyed

  Scenario: A sharee should not be able to delete a shared mailbox
    Given "bob@domain.tld" deletes the mailbox "shared" owned by "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailboxes should contain "shared" in "Personal" namespace

  Scenario: A sharee should not be able to create a shared mailbox child
    Given "bob@domain.tld" creates mailbox "sharedChild" with creationId "c-01" in mailbox "shared" owned by "alice@domain.tld"
    When "alice@domain.tld" lists mailboxes
    Then the mailboxes should contain "shared" in "Personal" namespace

  Scenario: A sharee should receive a not created response when trying to create a shared mailbox child
    When "bob@domain.tld" creates mailbox "sharedChild" with creationId "c-01" in mailbox "shared" owned by "alice@domain.tld"
    Then mailbox with creationId "c-01" is not created
