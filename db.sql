-- Creating a table that stores messages.
CREATE TABLE Messages (
  id serial NOT NULL,
  message text NOT NULL,
  PRIMARY KEY (id)
);

-- Table with user credentials.
CREATE TABLE Clients (
  id serial NOT NULL,
  login text NOT NULL UNIQUE,
  passHash bigint NOT NULL,
  passSalt bigint NOT NULL,
  sessionKey bigint NOT NULL,
  PRIMARY KEY (id)
);