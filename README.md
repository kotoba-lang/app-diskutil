# app-diskutil

**Disk Utility, on [`mokuroku`](https://github.com/kotoba-lang/mokuroku).**

Design: [ADR-2608035000](https://github.com/com-junkawasaki/root/blob/main/90-docs/adr/2608035000-app-standard-application-suite-on-a-shared-catalog-kernel.edn).

Capability: `system/metrics`. Nothing in this repo performs the effect — the host
supplies the provider function, and that is where the grant is spent.

## Three decisions

**Free space is never negative.** Providers disagree about whether reserved
blocks count as used, and some report used > capacity on a full APFS
container. A negative free space is never true and always looks like a bug in
the app rather than the provider.

**Unknown capacity is not 0%.** Showing zero would put an unreadable volume at
the safe end of a list sorted fullest-first, which is the opposite of a
warning. It is `nil`, and the kernel sorts absent values last in both
directions.

**Identity is the volume UUID, not the mount point.** The same disk mounts at
`/Volumes/Untitled 1` on the second plug-in, and an id that moves means Eject
can target the wrong device.

`system/metrics` is aggregate-only — reading how full a disk is does not
require the right to read what is on it, and this app never asks for
`fs/browse`. There is no Erase: no provider implements it, and a button that
either does nothing or does the worst possible thing is not worth offering.

## Test

```sh
kbb -M:local:test    # sibling checkouts
kbb -M:test          # pinned git deps
kbb -M:lint
```

design-quality: 100.00 on every window state including awaiting-grant (2026-08-03).
