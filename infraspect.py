import zipfile, os, socket, struct, pwd, json

java_version_cache = {}
java_vendor_map = {'Oracle Corporation' : 'oracle', 'N/A' : 'openjdk'}
hostname = socket.gethostname()

def java_home_version(java_home):
    """
    Determine the JVM version by insepcting rt.jar in the given Java home.
    """
    if java_home in java_version_cache:
        return java_version_cache[java_home]
    info = {}
    rt_jar_path = os.path.join(java_home, 'jre', 'lib', 'rt.jar')
    with zipfile.ZipFile(rt_jar_path, 'r') as rt_jar:
        with rt_jar.open('META-INF/MANIFEST.MF') as manifest:
            for line in manifest:
                line = line.strip()
                if not line: continue
                k, v = line.split(': ', 2)
                if k == 'Implementation-Version':
                    info['jvm_version'] = v
                elif k == 'Implementation-Vendor':
                    info['jvm_vendor'] = java_vendor_map.get(v, v)
    java_version_cache[java_home] = info
    return info

def java_home_for_pid(pid):
    """
    Locate the java home directory for a pid.
    """
    exe = os.readlink('/proc/%d/exe' % pid)
    home = os.path.dirname(os.path.dirname(exe))
    if os.path.basename(home) == 'jre':
        home = os.path.dirname(home)
    return home

def java_version(pid):
    """
    Determine the version of running JVM.
    """
    return java_home_version(java_home_for_pid(pid))


def slurp(path):
    """
    Read a while file as a string.
    """
    with open(path) as f:
        return f.read().strip()

def java_pid_iter():
    """
    Iterate over all running java processes.
    """
    for pid in os.listdir('/proc'):
        if not pid.isdigit(): continue
        try:
            if slurp('/proc/%s/comm' % pid) != 'java': continue
        except IOError:
            continue
        yield int(pid)

cmdline_options = {
    '-Dnla.node=': 'name',
    '-Djvmctl.node=': 'name',
    '-Xmx': 'heap_max',
    '-Xms': 'heap_min',
}

def parse_cmdline(pid):
    """
    Extract information from the java commandline.
    """
    info = {}
    cmdline = slurp('/proc/%d/cmdline' % pid).split('\0')
    for arg in cmdline:
        for prefix, option in cmdline_options.iteritems():
            if arg.startswith(prefix):
                info[option] = arg[len(prefix):]
    return info

def parse_hex_addr(addr):
    """
    Converts a hexadecimal host:port string to regular notation.
    """
    addr, port = addr.split(':')
    addr = socket.inet_ntoa(struct.pack("<L", int(addr, 16)))
    port = int(port, 16)
    return addr, port


def tcp_socket_iter():
    """
    Iterates over all TCP sockets on this host.
    """
    with open('/proc/net/tcp') as f:
        f.readline() # ignore header
        for line in f:
            line = line[:-1]
            fields = line.split()
            local_addr = parse_hex_addr(fields[1])
            peer_addr = parse_hex_addr(fields[2])
            yield {
                    'local_ip': local_addr[0],
                    'local_port': local_addr[1],
                    'remote_ip': peer_addr[0],
                    'remote_port': peer_addr[1],
                    'inode': int(fields[9]),
            }

tcp_sockets_by_inode = None

def file_handles(pid):
    global tcp_sockets_by_inode
    if tcp_sockets_by_inode is None:
        tcp_sockets_by_inode = {s['inode'] : s for s in tcp_socket_iter()}

    sockets = []
    files = []
    for fd in os.listdir('/proc/%d/fd' % pid):
        fd = int(fd)
        try:
            target = os.readlink('/proc/%d/fd/%d' % (pid, fd))
        except IOError:
            continue
        if target.startswith('socket:'):
            inode = int(target[8:-1])
            if inode in tcp_sockets_by_inode:
                sock_info = {
                'fd': fd,
                }
                sock_info.update(tcp_sockets_by_inode[inode])
                sockets.append(sock_info)
        elif target.startswith('/'):
            files.append({
                'fd': fd,
                'path': target
            })
    return sockets, files

def main():
    report = {
        'host': hostname,
    }
    jvms = []
    for pid in java_pid_iter():
        stat = os.stat("/proc/%d" % pid)
        sockets, files = file_handles(pid)
        info = {
                'pid': pid,
                'uid': stat.st_uid,
                'started': stat.st_mtime,
                'user': pwd.getpwuid(stat.st_uid)[0],
                'sockets': sockets,
                'files': files
        }
        info.update(parse_cmdline(pid))
        info.update(java_version(pid))
        jvms.append(info)
    report['jvms'] = jvms
    print json.dumps(report)

main()
