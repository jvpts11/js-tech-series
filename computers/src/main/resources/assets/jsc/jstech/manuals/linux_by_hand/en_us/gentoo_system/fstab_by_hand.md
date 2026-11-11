---
section: gentoo_system
order: 324
---
# The fstab by hand

The file system table, /etc/fstab, tells the system which partitions to mount at boot ([](jsc:arch_fstab)). The base starts with only a header in it, so you write the root line yourself.

The root line names the root partition by its universally unique identifier (UUID). The UUID belongs to one partition, and it stays the same when the disks are reordered ([](jsc:filesystems_and_mkfs)).

```steps
{"steps": ["Type blkid >> /etc/fstab and press [[Enter]].", "Type nano /etc/fstab and press [[Enter]].", "Find the line of the root partition, and cut it down to UUID=<its uuid>  /  ext4  noatime  0 1.", "Save the file and close nano."]}
```

Table: The fields of the root line

| Field | Value | What it means |
| --- | --- | --- |
| File system | `UUID=<its uuid>` | The root partition, by its identifier. |
| Mount point | `/` | The root folder of the system. |
| Type | `ext4` | The file system the partition is formatted with. |
| Options | `noatime` | Do not record when each file was last read. |
| Dump | `0` | Not backed up by the dump tool. |
| Pass | `1` | Checked first at boot. |

At reboot, the machine looks for a line whose mount point is /. It needs that line to find the root, so the reboot checks for it before it starts.

## If something goes wrong

> [!PROBLEM] The reboot says "no root filesystem in /etc/fstab".
> No line in the file mounts /. Write the root line as shown above, then reboot again ([](jsc:gentoo_reboot)).

> [!SEE] jsc:arch_fstab, jsc:gentoo_reboot
