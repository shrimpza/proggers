document.addEventListener("DOMContentLoaded", () => {

	// a unique subscriber for this session
	const s = Math.random()

	// get the current group, based on the path, otherwise fall back to "all"
	const groupMatch = /\/([a-zA-Z0-9_-]{1,32})\/?/
	const maybeGroup = window.location.pathname.match(groupMatch)
	const currentGroup = maybeGroup && maybeGroup.length > 1 ? maybeGroup[1] : "all"

	const bars = document.getElementById("bars");
	const barTemplate = document.getElementById("bar_template");
	const placeholder = bars.querySelector(".placeholder");

	const repeat = function() {
		fetch(`/progress/${currentGroup}?s=${s}`)
			.then(response => response.json())
			.then(updates => {
				// short circuit if nothing changed
				if (!updates || updates.length === 0) return

				updates
					.filter(u => u.updated)
					.map(u => u.updated)
					.sort((a, b) => {
						if (a.created < b.created) return -1;
						if (a.created > b.created) return 1;
						return 0;
					}).forEach(bar => {
					let current = document.getElementById(bar.id)
					if (!current) {
						current = createBar(bar)
						if (!bars.firstChild) bars.appendChild(current)
						else bars.firstChild.before(current)
					} else {
						updateBar(current, bar)
					}
				})

				updates.filter(u => u.deleted).map(u => u.deleted).forEach(gone => {
					let toRemove = document.getElementById(gone)
					if (toRemove) bars.removeChild(toRemove)
				})
			})
			.then(() => repeat())
	}

	const createBar = function(bar) {
		const clone = barTemplate.content.cloneNode(true).querySelector(".bar")

		clone.id = bar.id
		clone.querySelector("h2").textContent = bar.name.replaceAll("_", " ").replace(/(^\w)|(\s+\w)/g, l => l.toUpperCase())

		clone.addEventListener("click", () => {
			clone.querySelector(".info").classList.toggle("open")
		})

		return updateBar(clone, bar)
	}

	const updateBar = function(current, bar) {
		current.dataset.created = bar.created
		current.dataset.updated = bar.updated

		const outer = current.querySelector(".progress.out")
		const info = current.querySelector(".info")

		outer.style.setProperty("--c", `#${bar.color}`)
		outer.style.setProperty("--m", bar.max)
		outer.style.setProperty("--p", bar.progress)

		info.querySelector(".created").textContent = new Date(bar.created * 1000).toLocaleString()
		info.querySelector(".updated").textContent = new Date(bar.updated * 1000).toLocaleString()
		info.querySelector(".value").textContent = `${bar.progress} / ${bar.max} (${(bar.percent).toFixed(1)}%); ETA ${friendlyTime(bar.eta)}`

		return current
	}

	const friendlyTime = function(secs) {
		let totalSeconds = secs;
		let hours = Math.floor(totalSeconds / 3600);
		totalSeconds %= 3600;
		let minutes = Math.floor(totalSeconds / 60);
		let seconds = Math.ceil(totalSeconds % 60);

		return `${hours}h ${minutes}m ${seconds}s`;
	}

	// kick off the long polling cycle
	repeat()
});