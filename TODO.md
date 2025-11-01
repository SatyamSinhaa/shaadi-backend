# TODO: Implement Message Functionality Enhancements

## Tasks
- [x] Update Message.java: Add 'sentAt' timestamp field with @CreationTimestamp annotation for automatic population. Replace senderId and receiverId (int) with @ManyToOne relationships to User entity (sender and receiver).
- [x] Update MessageRepository.java: Change the query method from findBySenderIdOrReceiverId(int senderId, int receiverId) to findBySenderOrReceiver(User sender, User receiver) to work with User objects.
- [x] Update ChatService.java: Adjust methods to accept and return Message objects with User relationships; ensure proper handling of User entities in sendMessage, getMessagesForUser, etc.
- [x] Update ChatController.java: Modify endpoints to work with the updated Message structure; ensure request/response handling accommodates User relationships and add any necessary validation.
- [x] Test the updated endpoints to ensure messages can be sent and retrieved correctly.
- [x] Verify that the 'sentAt' timestamp is populated automatically upon message creation.
