package sv.edu.udb.retroresource.api

data class Resources(
    val id: Int?,
    val titulo: String,
    val enlace: String,
    val descripcion: String,
    val tipo: String,     // Tipo del recurso (ej. libro, video)
    val imagen: String    // Enlace de la imagen del recurso
)
