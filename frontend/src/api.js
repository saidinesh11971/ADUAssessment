const API_BASE = import.meta.env.VITE_API_BASE ?? "";

export async function importWorkbook(file) {
  const body = new FormData();
  body.append("file", file);
//abc
  const res = await fetch(`${API_BASE}/api/import`, {
    method: "POST",
    body,
  });
  //demo

  const contentType = res.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await res.json()
    : { message: await res.text() };

  return { status: res.status, ok: res.ok, payload };
}
