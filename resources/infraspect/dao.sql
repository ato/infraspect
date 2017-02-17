#region schema

CREATE TABLE jvm
(
  host VARCHAR(256) NOT NULL,
  name VARCHAR(256),
  pid INT(11) NOT NULL,
  heap_max VARCHAR(256),
  jvm_vendor VARCHAR(256),
  jvm_version VARCHAR(256),
  user VARCHAR(256),
  uid INT(11),
  PRIMARY KEY (host, pid)
);


CREATE TABLE file
(
  host VARCHAR(256) NOT NULL,
  pid INT(11) NOT NULL,
  fd INT(11) NOT NULL,
  path TEXT,
  PRIMARY KEY (host, pid, fd),
  CONSTRAINT file_jvm_host_pid_fk FOREIGN KEY (host, pid) REFERENCES jvm (host, pid) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE socket
(
  host VARCHAR(256) NOT NULL,
  pid INT(11) NOT NULL,
  fd INT(11) NOT NULL,
  local_ip VARCHAR(256) NOT NULL,
  local_port INT(11) NOT NULL,
  remote_ip VARCHAR(256) NOT NULL,
  remote_port INT(11) NOT NULL,
  PRIMARY KEY (host, pid, fd),
  CONSTRAINT socket_jvm_host_pid_fk FOREIGN KEY (host, pid) REFERENCES jvm (host, pid) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX socket_local_ip_local_port_index ON socket (local_ip, local_port);
CREATE INDEX socket_local_port_index ON socket (local_port);
CREATE INDEX socket_remote_ip_remote_port_index ON socket (remote_ip, remote_port);
CREATE INDEX socket_remote_port_index ON socket (remote_port);

#endregion

#region insertJvm
INSERT INTO jvm (host, name, pid, heap_max, jvm_vendor, jvm_version, user, uid)
VALUES
  (:host, :name, :pid, :heap_max, :jvm_vendor, :jvm_version,
   :user, :uid);
#endregion

#region deleteJvm
DELETE FROM jvm WHERE host = :host AND pid = :pid;
#endregion

#region insertFile
INSERT INTO file (host, pid, fd, path) VALUES (:host, :pid, :fd, :path);
#endregion

#region insertSocket
INSERT INTO socket (host, pid, fd, local_ip, local_port, remote_ip, remote_port)
VALUES (:host, :pid, :fd, :local_ip, :local_port, :remote_ip, :remote_port);
#endregion