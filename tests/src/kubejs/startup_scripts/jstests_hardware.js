// A part a modpack declares from a startup script, as an object of its sheet's fields: the test mod's memory
// module becomes an Advanced DDR5 module of 4 096 items, which the GameTests read back from the registry.
JsComputers.memory('jstests:scripted_ram', { era: 'advanced', generation: 'ddr5', buffer_items: 4096, tdp_watts: 3 })
